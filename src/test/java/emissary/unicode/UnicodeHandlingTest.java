package emissary.unicode;

import emissary.core.IBaseDataObjectXmlCodecs;
import emissary.util.ByteUtil;

import com.ctc.wstx.api.WstxInputProperties;
import com.ctc.wstx.exc.WstxParsingException;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The purpose of this unit test is to compare various unicode escaping and identification strategies.
 * <p/>
 * The terms "printable" and "valid text" are ambiguous without further context. There are several use cases that might
 * prefer different algorithms for escaping/encoding.
 * <p/>
 * Some examples:
 * <ul>
 * <li>Escaping the keys and values to create valid JSON</li>
 * <li>Escaping the keys and values to create valid XML 1.0</li>
 * <li>Escaping the keys and values to create valid XML 1.1</li>
 * <li>Escaping values in the Regression test answer XML files</li>
 * </ul>
 */
class UnicodeHandlingTest {
    protected static final Logger logger = LoggerFactory.getLogger(UnicodeHandlingTest.class);

    protected static final String COLLECTOR_KEY = "collector_name";
    protected static final String TOTAL_KEY = "total";
    protected static final String ESCAPE_KEY = "wants_escape_count";

    protected static final String DATA_KEY = "utf8bytes";
    protected static final String CODEPOINT_LIST_KEY = "codepoints_needing_escape";

    protected static final int UNICODE_TOTAL = 1114112;


    @Test
    void runHarnessJustUnicodePointByPoint() {

        Map<String, Object> collectorICU4J = new HashMap<>();
        Map<String, Object> collectorJavaControl = new HashMap<>();
        Map<String, Object> collectorDave = new HashMap<>();
        Map<String, Object> collectorFrank = new HashMap<>();
        Map<String, Object> collectorLisa = new HashMap<>();
        Map<String, Object> collectorJSON = new HashMap<>();
        Map<String, Object> collectorXML = new HashMap<>();


        for (UnicodeSetIterator it = new UnicodeSetIterator(UnicodeSet.ALL_CODE_POINTS); it.next();) {
            int codepoint = it.codepoint;
            byte[] utf8bytes = it.getString().getBytes(StandardCharsets.UTF_8);

            icu4jHandler(null, codepoint, collectorICU4J);
            javaControlHandler(null, codepoint, collectorJavaControl);
            daveHandler(utf8bytes, codepoint, collectorDave);
            frankHandler(utf8bytes, codepoint, collectorFrank);
            lisaHandler(utf8bytes, codepoint, collectorLisa);
            jsonStandardHandler(utf8bytes, codepoint, collectorJSON);
            xmlStandardHandler(utf8bytes, codepoint, collectorXML);

        }

        displayStats(collectorJavaControl);
        displayStats(collectorICU4J);
        displayStats(collectorDave);
        displayStats(collectorFrank);
        displayStats(collectorLisa);
        displayStats(collectorXML);
        displayStats(collectorJSON);

    }


    /**
     * If you were to put a cursor beside this String, how many times would you have to press right (or left) arrow to
     * traverse it?
     * 
     * @param text to traverse
     * @return count of arrow key presses
     */
    int graphemeCount(String text) {
        BreakIterator it = BreakIterator.getCharacterInstance();
        it.setText(text);
        int count = 0;
        while (it.next() != BreakIterator.DONE) {
            count++;
        }
        return count;
    }


    String createSomeData(int codePoint, boolean moreThanOnce, String addThisPrefix, String addInTheMiddle, String addThisSuffix) {
        StringBuilder sb = new StringBuilder();
        if (addThisPrefix != null) {
            sb.append(addThisPrefix);
        }
        sb.appendCodePoint(codePoint);
        if (addInTheMiddle != null) {
            sb.append(addInTheMiddle);
        }
        if (moreThanOnce) {
            sb.appendCodePoint(codePoint);
        }
        if (addThisSuffix != null) {
            sb.append(addThisSuffix);
        }
        return sb.toString();
    }


    byte[] convertStringToUtf8Bytes(String data) {
        return data.getBytes(StandardCharsets.UTF_8);
    }


    void icu4jHandler(byte[] utf8bytes, int codepoint, Map<String, Object> collector) {
        collector.put(COLLECTOR_KEY, "ICU4J");
        incrementCount(collector, TOTAL_KEY);
        if (UCharacter.isISOControl(codepoint) && !UCharacter.isWhitespace(codepoint)) {
            incrementCount(collector, ESCAPE_KEY);
            addToCodePointList(collector, codepoint);
        }
    }

    void javaControlHandler(byte[] utf8bytes, int codepoint, Map<String, Object> collector) {
        collector.put(COLLECTOR_KEY, "JAVA");
        incrementCount(collector, TOTAL_KEY);


        if (Character.isISOControl(codepoint) && !Character.isWhitespace(codepoint)) {
            incrementCount(collector, ESCAPE_KEY);
            addToCodePointList(collector, codepoint);
        }


    }

    void daveHandler(byte[] utf8bytes, int codepoint, Map<String, Object> collector) {
        collector.put(COLLECTOR_KEY, "DAVE");
        incrementCount(collector, TOTAL_KEY);

        if (IBaseDataObjectXmlCodecs.requiresEncoding(utf8bytes)) {
            incrementCount(collector, ESCAPE_KEY);
            addToCodePointList(collector, codepoint);
        }
    }

    void frankHandler(byte[] utf8bytes, int codepoint, Map<String, Object> collector) {
        collector.put(COLLECTOR_KEY, "FRANK");
        incrementCount(collector, TOTAL_KEY);

        if (ByteUtil.hasNonPrintableValuesPR1001(utf8bytes)) {
            incrementCount(collector, ESCAPE_KEY);
            addToCodePointList(collector, codepoint);
        }

    }

    void lisaHandler(byte[] utf8bytes, int codepoint, Map<String, Object> collector) {
        collector.put(COLLECTOR_KEY, "LISA");
        incrementCount(collector, TOTAL_KEY);

        if (!Character.isValidCodePoint(codepoint)) {
            incrementCount(collector, ESCAPE_KEY);
            addToCodePointList(collector, codepoint);
        }

    }


    void jsonStandardHandler(byte[] utf8bytes, int codepoint, Map<String, Object> collector) {
        collector.put(COLLECTOR_KEY, "JSON");
        incrementCount(collector, TOTAL_KEY);
    }


    void xmlStandardHandler(byte[] utf8bytes, int codepoint, Map<String, Object> collector) {
        collector.put(COLLECTOR_KEY, "XML");
        incrementCount(collector, TOTAL_KEY);

    }


    void incrementCount(Map<String, Object> collector, String key) {
        if (!collector.containsKey(key)) {
            collector.put(key, 1);
        } else {
            int val = (int) collector.get(key);
            collector.put(key, val + 1);
        }
    }

    void addToCodePointList(Map<String, Object> collector, int codepoint) {
        if (!collector.containsKey(CODEPOINT_LIST_KEY)) {
            List<Integer> cps = new ArrayList<>();
            cps.add(codepoint);
            collector.put(CODEPOINT_LIST_KEY, cps);
        } else {
            @SuppressWarnings("unchecked")
            List<Integer> cps = (List<Integer>) collector.get(CODEPOINT_LIST_KEY);
            cps.add(codepoint);
            collector.put(CODEPOINT_LIST_KEY, cps);
        }
    }


    void displayStats(Map<String, Object> collector) {
        StringBuffer sb = new StringBuffer();
        sb.append("\n===============\n");
        sb.append(collector.get(COLLECTOR_KEY));
        sb.append("\n");
        if (collector.containsKey(TOTAL_KEY)) {
            if (UNICODE_TOTAL != (int) collector.get(TOTAL_KEY)) {
                sb.append("Total Codepoints Evaluated less than the total: " + (UNICODE_TOTAL - (int) collector.get(TOTAL_KEY)));
                sb.append("\n");
            }
        }
        if (collector.containsKey(ESCAPE_KEY)) {
            sb.append("Total Needing Escape: " + collector.get(ESCAPE_KEY));
            sb.append("\n");
        }
        if (collector.containsKey(CODEPOINT_LIST_KEY)) {
            sb.append("List of CodePoints Needing Escape: " + prettyFormatList(collector.get(CODEPOINT_LIST_KEY)));
            sb.append("\n");
        }

        logger.info(sb.toString());
    }

    String prettyFormatList(Object list) {
        @SuppressWarnings("unchecked")
        List<Integer> codepointList = (List<Integer>) list;
        if (codepointList.size() > 1000) {
            return "greater than 1000";
        }
        return codepointList.stream().map(i -> i.toString()).collect(Collectors.joining(", "));

    }


    /**
     * The string returned from this method contains a single emoji (one graphical unit) consisting of five Unicode scalar
     * values.
     * <p>
     * First, there’s a base character that means a person face palming. By default, the person would have a cartoonish
     * yellow color.
     * <p>
     * The next character is an emoji skintone modifier the changes the color of the person’s skin (and, in practice, also
     * the color of the person’s hair). By default, the gender of the person is undefined, and e.g. Apple defaults to what
     * they consider a male appearance and e.g. Google defaults to what they consider a female appearance.
     * <p>
     * The next two scalar values pick a male-typical appearance specifically regardless of font and vendor. Instead of
     * being an emoji-specific modifier like the skin tone, the gender specification uses an emoji-predating gender symbol
     * (MALE SIGN) explicitly ligated using the ZERO WIDTH JOINER with the (skin-toned) face-palming person. (Whether it is
     * a good or a bad idea that the skin tone and gender specifications use different mechanisms is out of the scope of
     * this post.)
     * <p>
     * Finally, VARIATION SELECTOR-16 makes it explicit that we want a multicolor emoji rendering instead of a monochrome
     * dingbat rendering.
     * 
     * @See {@link <a href="https://hsivonen.fi/string-length/">https://hsivonen.fi/string-length/</a>}
     * 
     * @return a java string representing the emoji (a single glyph as displayed to a human)
     */
    String getAComplicatedEmoji() {

        StringBuilder sb = new StringBuilder();

        // U+1F926 FACE PALM - in Java, needs to be represented as 2 UTF-16 code units (surrogate pairs) and 4 UTF-8
        // code units
        // sb.append('\u1F926'); No - use the lookup to find the UTF-16 encoding break down:
        // https://www.compart.com/en/unicode/U+1F926
        sb.append('\uD83E');
        sb.append('\uDD26');

        // U+1F3FC EMOJI MODIFIER FITZPATRICK TYPE-3 - in Java, needs to be represented as 2 UTF-16 code units
        // (surrogate pairs) and 4 UTF-8 code units
        // sb.append('\u1F3FC'); No - use the lookup to find the UTF-16 encoding break down:
        // https://www.compart.com/en/unicode/U+1F3FC
        sb.append('\uD83C');
        sb.append('\uDFFC');

        // U+200D ZERO WIDTH JOINER - in Java, needs to be represented as 1 UTF-16 code units and 3 UTF-8 code units
        sb.append('\u200D');

        // U+2642 MALE SIGN - in Java, needs to be represented as 1 UTF-16 code units and 3 UTF-8 code units
        sb.append('\u2642');

        // U+FE0F VARIATION SELECTOR-16 - in Java, needs to be represented as 1 UTF-16 code unit and 3 UTF-8 code units
        sb.append('\uFE0F');

        String facePalmDude = sb.toString();

        assertEquals(17, facePalmDude.getBytes(StandardCharsets.UTF_8).length, "There should be 17 UTF-8 bytes");

        assertEquals(14, facePalmDude.getBytes(StandardCharsets.UTF_16BE).length, "There should be 7 UTF-16 units at 2 byte each");

        assertEquals(7, facePalmDude.length(), "Number of UTF-16 code units");

        assertEquals(5, facePalmDude.codePointCount(0, facePalmDude.length()), "Number of UTF-32 code units, there are 5 Unicode scalar values ");

        assertEquals(1, graphemeCount(facePalmDude), "There is only one grapheme/emoji/glyph.");

        Normalizer2 nfcNormalizer = Normalizer2.getNFCInstance();
        assertTrue(nfcNormalizer.isNormalized(facePalmDude), "is NFC normalized");

        Normalizer2 nfdNormalizer = Normalizer2.getNFDInstance();
        assertTrue(nfdNormalizer.isNormalized(facePalmDude), "is NFD normalized");

        Normalizer2 nfckcaseNormalizer = Normalizer2.getNFKCCasefoldInstance();
        assertFalse(nfckcaseNormalizer.isNormalized(facePalmDude), "is NFKC Casefold normalized");

        Normalizer2 nfckNormalizer = Normalizer2.getNFKCInstance();
        assertTrue(nfckNormalizer.isNormalized(facePalmDude), "is NFCK normalized");

        Normalizer2 nfdkNormalizer = Normalizer2.getNFKDInstance();
        assertTrue(nfdkNormalizer.isNormalized(facePalmDude), "is NFDK normalized");

        return facePalmDude;

    }


    @Test
    void reproduceXMLParsingSurrogatePairsException() {
        // (if no emoji font installed, the last character should be displayed as santa claus emoji U+1F385)
        final String inputElement = "<value>Merry Christmas &#55356;&#57221;</value>";
        WstxInputFactory wstxInputFactory = new WstxInputFactory();
        wstxInputFactory.configureForSpeed();

        WstxParsingException thrown = assertThrows(WstxParsingException.class,
                () -> {

                    doTheParsing(wstxInputFactory, inputElement, "");

                });

        assertTrue(thrown.getMessage().contains("Illegal character entity: expansion character"));

    }

    @Disabled
    @Test
    /**
     * @See <a href= "https://github.com/FasterXML/woodstox/pull/174/">https://github.com/FasterXML/woodstox/pull/174/</a>
     * @throws XMLStreamException
     */
    void testMoreXmlSurrogatePairs() throws XMLStreamException {

        WstxInputFactory wstxInputFactory = new WstxInputFactory();
        wstxInputFactory.configureForSpeed();
        wstxInputFactory.setProperty(WstxInputProperties.P_ALLOW_SURROGATE_PAIR_ENTITIES, true);
        int assertions = 0;

        Map<String, String> validSurrogatePairs = getValidSurrogateDataSet();

        for (Entry<String, String> xmlExp : validSurrogatePairs.entrySet()) {
            doTheParsing(wstxInputFactory, xmlExp.getKey(), xmlExp.getValue());
            assertions++;
        }
        assertEquals(validSurrogatePairs.size(), assertions, "Expected to pass all the test cases.");

    }


    void doTheParsing(WstxInputFactory wstxInputFactory, String inputElement, String expected) throws XMLStreamException {
        final ByteArrayInputStream is = new ByteArrayInputStream(inputElement.getBytes(StandardCharsets.UTF_8));

        final XMLStreamReader reader = wstxInputFactory.createXMLStreamReader(is);

        assertEquals(XMLStreamConstants.START_ELEMENT, reader.next());
        assertEquals(XMLStreamConstants.CHARACTERS, reader.next());

        StringBuffer sb = new StringBuffer(reader.getText());

        while (reader.next() == XMLStreamConstants.CHARACTERS) {
            sb.append(reader.getText());
        }

        String result = sb.toString();
        assertEquals(expected, result);


        reader.close();
    }


    Map<String, String> getValidSurrogateDataSet() {
        final Map<String, String> xmlWithSurrogatePairs = new HashMap<String, String>();
        // Numeric surrogate pairs
        xmlWithSurrogatePairs.put("<root>surrogate pair: &#55356;&#57221;.</root>",
                "surrogate pair: \uD83C\uDF85.");
        // Hex and numeric surrogate pairs
        xmlWithSurrogatePairs.put("<root>surrogate pair: &#xD83C;&#57221;.</root>",
                "surrogate pair: \uD83C\uDF85.");
        // Numeric and hex surrogate pairs
        xmlWithSurrogatePairs.put("<root>surrogate pair: &#55356;&#xDF85;.</root>",
                "surrogate pair: \uD83C\uDF85.");
        // Hex surrogate pairs
        xmlWithSurrogatePairs.put("<root>surrogate pair: &#xD83C;&#xDF85;.</root>",
                "surrogate pair: \uD83C\uDF85.");
        // Two surrogate pairs
        xmlWithSurrogatePairs.put("<root>surrogate pair: &#55356;&#57221;&#55356;&#57220;.</root>",
                "surrogate pair: \uD83C\uDF85\uD83C\uDF84.");
        // Surrogate pair and simple entity
        xmlWithSurrogatePairs.put("<root>surrogate pair: &#55356;&#57221;&#8482;.</root>",
                "surrogate pair: \uD83C\uDF85\u2122.");

        return xmlWithSurrogatePairs;
    }


}
