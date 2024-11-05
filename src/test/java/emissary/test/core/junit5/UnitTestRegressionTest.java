package emissary.test.core.junit5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import emissary.place.IServiceProviderPlace;
import emissary.util.ByteUtil;

class UnitTestRegressionTest {
	protected static final Logger logger = LoggerFactory.getLogger(UnitTestRegressionTest.class);

	RegressionTest subject = new RhinoRegressionTest();

	@Test
	void testByteUtils_Using_ALL_CODE_POINTS() {
		int total = 0;
		int byteUtil_mismatched = 0;
		int java_mismatched = 0;
		int error = 0;

		for (UnicodeSetIterator it = new UnicodeSetIterator(UnicodeSet.ALL_CODE_POINTS); it.next();) {
			total++;
			boolean byteUtilShouldEncode = false;
			boolean icu4jDeclaresThisAString = false;

			int codepoint = it.codepoint;

			String strFromIterator = it.getString();
			char[] chars = Character.toChars(codepoint); // BMP will have len=1, supplements will result in len=2
			String strFromCodePoint = new String(chars);

			if (!StringUtils.equals(strFromIterator, strFromCodePoint)) {
				logger.info("These are NOT equal: {} and {}", strFromIterator, strFromCodePoint);
				error++;
			}

			boolean javaDeclaresThisAControl = Character.isISOControl(codepoint);
			byteUtilShouldEncode = ByteUtil.shouldEncode(strFromCodePoint.getBytes(StandardCharsets.UTF_8));

			if (UnicodeSetIterator.IS_STRING == codepoint) {
				icu4jDeclaresThisAString = true;
			}

			if (javaDeclaresThisAControl && !byteUtilShouldEncode) {
				logger.info("java says this is a control: {}", codepoint);
				java_mismatched++;
			}

			if (icu4jDeclaresThisAString && byteUtilShouldEncode) {
				logger.info("ICU4J says this doesn't need escaped: {}", codepoint);
			}

			if (!icu4jDeclaresThisAString && !byteUtilShouldEncode) {
				logger.info("ICU4J says this should be escaped: {}", codepoint);
				byteUtil_mismatched++;
			}

		}
		logger.info("There were an error: {}", error);

		stat("Checked {} code points, {} were mismatched, {} were NOT mismatched, {} java mismatched", total,
				byteUtil_mismatched, java_mismatched);
	}

	public void stat(String format, int total, int mismatched, int other) {
		logger.info(format, total, mismatched, total - mismatched, other);
	}

	@Test
	void testHashBytesIfNonPrintableUsingALL_CODE_POINTS() {
		int total = 0;
		int mismatched = 0;
		int other = 0;

		Optional<String> result;
		for (UnicodeSetIterator it = new UnicodeSetIterator(UnicodeSet.ALL_CODE_POINTS); it.next();) {
			total++;
			boolean weWantToDisplayIt = true;

			boolean icu4jDeclaresThisAString = false;

			int codepoint = it.codepoint;

			char[] chars = Character.toChars(codepoint); // BMP will have len=1, supplements will result in len=2

			String thisCodePointAsString = new String(chars);

			boolean javaDeclaresThisAControl = Character.isISOControl(codepoint);

			result = subject.hashBytesIfNonPrintable(thisCodePointAsString.getBytes(StandardCharsets.UTF_8));

			if (UnicodeSetIterator.IS_STRING == codepoint) {
				icu4jDeclaresThisAString = true;
			}

			if (result.isPresent()) {
				weWantToDisplayIt = false;
			}

			if (javaDeclaresThisAControl && weWantToDisplayIt) {
				logger.info(
						"\nJava says this is a control codepoint but we want to display it:\n codepoint '{}' string '{}'\n has {} characters",
						codepoint, thisCodePointAsString, chars.length);
			}

			if (!icu4jDeclaresThisAString && weWantToDisplayIt) {
				mismatched++;
				logger.info(
						"\nICU4J says this is NOT a string \nbut we want to display it:\n codepoint '{}' string '{}' \n has {} characters",
						codepoint, thisCodePointAsString, chars.length);

			}

			if (!weWantToDisplayIt && icu4jDeclaresThisAString) {
				other++;
				logger.info(
						"\nICU4j says this IS a string, but we still want to hash it: codepoint '{}' string '{}'\n has {} characters",
						codepoint, thisCodePointAsString, chars.length);
			}

		}
		logger.info("Checked {} code points, {} were mismatched, {} were NOT mismatched, {} other", total, mismatched,
				total - mismatched, other);

	}

	class RhinoRegressionTest extends RegressionTest {
		@Override
		public IServiceProviderPlace createPlace() throws IOException {
			return null;
		}
	}

}
