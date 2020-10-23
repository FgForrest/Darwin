package one.edee.darwin.utils;

import java.util.regex.Pattern;

/**
 * Class for processing script strings
 * @author ZMA
 * Date: 28.1.2010
 * Time: 8:24:36
 */
public class StringProcessor {

	private StringProcessor() {
	}

	/**
     * Removes comments from content
     *
     * @param content to be precessed
     * @return clarified content without comments
     */
    public static String removeCommentsFromContent(String content) {
        if(content==null) {
            return null;
        }
        String processed = Pattern.compile("^\\s*+#.+?$", Pattern.MULTILINE).matcher(content).replaceAll("");
        processed = Pattern.compile("^\\s*+--.+?$", Pattern.MULTILINE).matcher(processed).replaceAll("");
        processed = Pattern.compile("^\\s*+/\\*.+?\\*/", Pattern.DOTALL | Pattern.MULTILINE).matcher(processed).replaceAll("");
        return processed;
    }

}
