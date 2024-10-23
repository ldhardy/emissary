package emissary.test.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SimpleUnitTestUtilTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleUnitTestUtilTest.class);

    @Test
    @DisplayName("ECHIDNA STAT")
    void testGetSetOfFilesForClassWithGivenSuffix() {
        String suffixOfInterest = ".stat";
        int expectedFileCount = 5;
        Set<Entry<String, String>> getSome =
                SimpleUnitTestUtil.getSetOfFilesForClassWithGivenSuffix(SimpleUnitTestUtilTest.class, suffixOfInterest);

        assertEquals(expectedFileCount, getSome.size());

        int i = 0;
        for (Entry<String, String> entry : getSome) {
            logger.debug("{}, {}", entry.getKey(), entry.getValue());

            // shortname as key
            assertEquals("echidna" + (++i), entry.getKey(), "The key should be the shortname of the file but was: " + entry.getKey());

            // file type is the right kind
            assertTrue(entry.getValue().endsWith(suffixOfInterest),
                    "The value should be a path that ends with the suffix of interest, path: "
                            + entry.getValue() + " | suffix of interest: " + suffixOfInterest);

            // file path is absolute, not relative
            assertTrue(entry.getValue().startsWith("/"),
                    "The value should be an absolute path, but was: " + entry.getValue());

            // file path absolute path is correct and file exists
            File fqfn = new File(entry.getValue());
            assertTrue(fqfn.exists(), "Absolute file path should exist: " + fqfn.toString());
        }

        if (i < expectedFileCount) {
            fail("The validation loop didn't run correctly.  Should have looped " + expectedFileCount + " times but only looped " + i);
        }

    }

    @Test
    @DisplayName("ECHIDNA BLAT")
    void testGetSetOfFilesForClassWithGivenSuffix_suffixDoesntExist() {
        String suffixOfInterest = ".blat";
        Set<Entry<String, String>> getSome =
                SimpleUnitTestUtil.getSetOfFilesForClassWithGivenSuffix(SimpleUnitTestUtilTest.class, suffixOfInterest);

        assertEquals(0, getSome.size(), "There should be zero files with suffix and the return set should not be null.");


    }

    @Test
    @DisplayName("PUFF POOF")
    void testGetExpectedOutputContentByResourcePath() throws IOException {
        String inputSuffix = ".poof";
        String expectedSuffix = ".stat";

        Set<Entry<String, String>> magicDragon =
                SimpleUnitTestUtil.getSetOfFilesForClassWithGivenSuffix(SimpleUnitTestUtilTest.class, inputSuffix);

        for (Entry<String, String> entry : magicDragon) {

            String statContent = SimpleUnitTestUtil.getExpectedOutputContentByResourcePath(entry.getValue(), inputSuffix, expectedSuffix);

            assertEquals("LIVES BY THE SEA", statContent);
        }
    }

    @Test
    @DisplayName("PUFF GIVE")
    void testGetExpectedOutputContentByResourcePath_doesntExist() throws IOException {
        String inputSuffix = ".poof";
        String expectedSuffix = ".give";
        int expectedPoofCount = 1;

        Set<Entry<String, String>> magicDragon =
                SimpleUnitTestUtil.getSetOfFilesForClassWithGivenSuffix(SimpleUnitTestUtilTest.class, inputSuffix);

        assertEquals(expectedPoofCount, magicDragon.size());

        int i = 0;
        for (Entry<String, String> entry : magicDragon) {
            i++;
            IOException thrown = Assertions.assertThrows(IOException.class, () -> {
                SimpleUnitTestUtil.getExpectedOutputContentByResourcePath(entry.getValue(), inputSuffix, expectedSuffix);
            }, "IOException was expected, file *.give does not exist.");

            Assertions.assertEquals("useful error message", thrown.getMessage());

        }

        assertEquals(expectedPoofCount, i, "There should have been no entries");
    }

}
