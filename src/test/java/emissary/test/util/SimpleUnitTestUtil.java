package emissary.test.util;

import emissary.util.io.ResourceReader;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("rawtypes")
public class SimpleUnitTestUtil {

    // This class should only ever have static members
    private SimpleUnitTestUtil() {}

    public static final String INPUT_FILE_SUFFIX = ".in";
    public static final String EXPECTED_FILE_SUFFIX = "-expected.txt";
    public static final String ACTUAL_FILE_SUFFIX = "-actual.txt";

    /**
     * This helper method returns a set of entries, where each entry contains the file prefix as the key and the input
     * file's absolute path as the value.
     * <p>
     * For example, if there were a directory <code>src/test/resources/package/FancyPantsPlaceTest/</code> and it
     * contained
     * <ul>
     * <li><code>candysequins.in</code></li>
     * <li><code>candysequins-expected.txt</code></li>
     * <li><code>rainbowtearaway.in</code></li>
     * <li><code>rainbowtearaway-expected.txt</code></li>
     * </ul>
     * And you executed this method like so:
     * 
     * {@code getSetOfFilesForClassWithGivenSuffix(FancyPantsPlaceTest.class, '.in')}
     * 
     * the result would be a Set of Entries,
     * <ul>
     * <li><code>Entry<'candysequins','/full/path/emissaryclone/target/test-classes/package/FancyPantsPlaceTest/candysequins.in'></code></li>
     * <li><code>Entry<'rainbowtearaway','/full/path/emissaryclone/target/test-classes/package/FancyPantsPlaceTest/rainbowtearaway.in' ></code></li>
     * </ul>
     * 
     * @param testClassName where the classloader should look in the classpath for files with the appropriate suffix.
     * @param inputFileSuffix the suffix for the files of interest. See {@link #INPUT_FILE_SUFFIX}
     * @return a sorted set of entries for this test class where the key of each entry is the test file's short name
     *         with no suffix and the value of each entry is the absolute path of the file. This set becomes useful
     *         when used in combination with other methods in this utility class. See
     *         {@link #getFilePrefixToAbsolutePathBySuffixMap(String, String)} and
     *         {@link #writeOutput(String, String, String, String)}
     * 
     * @See Similar functionality {@link #getFilePrefixToAbsolutePathBySuffixMap(String, String)}
     */
    public static Set<Entry<String, String>> getSetOfFilesForClassWithGivenSuffix(Class testClassName, String inputFileSuffix) {
        ResourceReader rr = new ResourceReader();

        List<String> rs = rr.findResourcesFor(testClassName, inputFileSuffix);
        Set<Entry<String, String>> entries = new TreeSet<>();
        for (String r : rs) {
            int pos = r.lastIndexOf("/");
            String name = r.substring(pos + 1).replace(inputFileSuffix, "");
            File fqfn = new File(r);
            entries.add(new SimpleUnitTestEntry(name, fqfn.getAbsolutePath()));
        }
        return entries;
    }

    /**
     * This is a utility to pass in a resource path string and return the CONTENT of the expected answer file. You'll
     * need to specify which input file path portion to replace with which "expected" file path portion. This method
     * will read the contents of that file and return the contents.
     * 
     * @param filePath - the path, usually of the file containing the input data.
     * @param existingSuffix - the file suffix of the input file.
     * @param replaceWithSuffix - the file suffix of the expected or output file to read content from.
     * @return the content from the expected/answer file
     * @throws IOException if the expected file doesn't exist.
     *         <p>
     *         For example,
     *         {@code getExpectedOutputContentByResourcePath('/full/path/to/myclonedproject/module/target/test-classes/package/FancyPantsPlaceTest/candysequins.in','.in','.out');}
     *         will return the contents of the file found at
     *         <code>/full/path/to/myclonedproject/module/target/test-classes/package/FancyPantsPlaceTest/candysequins.out'</code>
     *         or throw an exception if that file doesn't exist.
     */
    public static String getExpectedOutputContentByResourcePath(String filePath, String existingSuffix, String replaceWithSuffix) throws IOException {
        File f = new File(filePath);
        File fqfp = f.getAbsoluteFile();
        String fname = f.getName();
        String expectedFile = fname.replace(existingSuffix, replaceWithSuffix);
        File expectedPathAbs = new File(fqfp.getParent(), expectedFile).getAbsoluteFile();
        if (!expectedPathAbs.exists()) {
            throw new IOException("Path does not exist: " + expectedPathAbs);
        }
        String expectedPath = expectedPathAbs.toString();
        ResourceReader rr = new ResourceReader();
        InputStream doc = rr.getResourceAsStream(expectedPath);
        return IOUtils.toString(doc, Charset.defaultCharset());
    }


    /**
     * This helper method will help locate the expected output for a simple unit test.
     * 
     * Use this method in combination with the {@link #getFilePrefixToAbsolutePathBySuffixMap(String, String)} method to
     * find the expected output for the input files. Feel free to use the static constant {@link #EXPECTED_FILE_SUFFIX}
     * as a convention for the fileSuffix, but it is not required.
     * 
     * @param className (e.g. 'FancyPantsPlaceTest')
     * @param filePrefix (e.g. 'candysequins')
     * @param expectedFileSuffix (e.g. '-expected.txt')
     * @return the abslute path to a file containing the expected content. In the given example, something like
     *         <code>/full/path/to/myclonedproject/module/target/test-classes/package/FancyPantsPlaceTest/candysequins-expected.txt</code>.
     * 
     */
    public static String getExpectedOutputFilenameByClassloaderAndPrefixMatch(Class className, String filePrefix, String expectedFileSuffix) {
        ResourceReader rr = new ResourceReader();
        List<String> rs = rr.findResourcesFor(className, expectedFileSuffix);
        for (String r : rs) {
            int pos = r.lastIndexOf("/");
            String name = r.substring(pos + 1);
            if (name.startsWith(filePrefix)) {
                return r;
            }
        }
        return null;
    }


    /**
     * This is a utility for writing out files. Feel free to use the constant {@link #ACTUAL_FILE_SUFFIX} to write
     * output during the test, but it is not required.
     * 
     * @param outputDir - which directory to write the output file (e.g. 'target/my-unit-test/case1')
     * @param outputContent - what content to write to the file
     * @param filenamePrefix - the beginning part of the outputfile (e.g. 'example1')
     * @param fileSuffix - the ending part of hte output file (e.g. '-actual.out')
     *        <p>
     *        With the given examples, a file would be written to
     *        <code>target/my-unit-test/case1/example1-actual.out</code> containing the content provided in
     *        <code>outputContent</code>
     *        </p>
     */
    public static void writeOutput(String outputDir, String outputContent, String filenamePrefix, String fileSuffix) {
        File f = new File(outputDir);

        if (!f.isDirectory()) {
            throw new IllegalStateException("Unable to create output directory " + f);
        }
        File output = new File(f, filenamePrefix + fileSuffix);
        try {
            Writer fw = Files.newBufferedWriter(output.toPath(), StandardCharsets.UTF_8);
            PrintWriter pw = new PrintWriter(fw);
            pw.write(outputContent);
            pw.close();

        } catch (IOException ex) {
            throw new IllegalStateException("IOException writing output file " + output, ex);
        }
    }

    /**
     * This is a utility for retrieving files at a specific directory that have a specific suffix. It will not recurse
     * into subdirectories. This is different than {@link #getSetOfFilesForClassWithGivenSuffix(Class, String)} in that
     * class will find all resources for the given class on the classpath where this one will only search the given
     * directory at one level.
     * 
     * @param path - the directory in which to locate files.
     * @param suffix - the suffix of files of interest. See {@link #INPUT_FILE_SUFFIX}, {@link #EXPECTED_FILE_SUFFIX},
     *        {@link #ACTUAL_FILE_SUFFIX}. But this can be any string.
     * @return a map that contains the shortname of the file and the full path to the file. e.g. if there is directory
     *         <code>target/myTest</code> and it contains
     *         <ul>
     *         <li><code>actualOutput1.txt</code></li>
     *         <li><code>actualOutput2.xml<code></li>
     *         <li><code>actualOutput2.txt<code></li>
     *         <li><code>actualOutput2.pdf<code></li>
     *         </ul>
     *         You call <code>SimpleUnitTestUtil.getFileMap("target/myTest", ".txt")</code> would return a map that
     *         contains
     *         <ul>
     *         <li><code>actualOutput1, /home/dir/src/project/core/target/myTest/actualOutput1.txt</code></li>
     *         <li><code>actualOutput2, /home/dir/src/project/core/target/myTest/actualOutput2.txt</code></li>
     *         </ul>
     * 
     *         If there are no files with the expected suffix in the directory, then an empty Map will be returned. This
     *         method will never return null.
     */
    public static Set<Entry<String, String>> getFilePrefixToAbsolutePathBySuffixMap(String path, String suffix) {
        Set<Entry<String, String>> entries = new TreeSet<>();
        File dir = new File(path);
        String fqPath = dir.getAbsolutePath();
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) {
                Arrays.stream(list).filter(s -> s.endsWith(suffix))
                        .forEach(s -> entries.add(new SimpleUnitTestEntry(s.replace(suffix, ""), fqPath + '/' + s)));
            }
        }
        return entries;
    }


}
