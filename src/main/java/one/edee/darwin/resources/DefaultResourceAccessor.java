package one.edee.darwin.resources;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.ResourceVersionComparator;
import org.apache.commons.io.IOUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Default implementation of {@link ResourceAccessor}.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@Data
@CommonsLog
public class DefaultResourceAccessor implements ResourceLoaderAware, ResourceAccessor {
	private static final String DESCRIPTOR_FILE = "descriptor.txt";
	private static final char SEMICOLON = ';';
	private static final char SINGLE_APOSTROPHE = '\'';
	private static final char DOUBLE_APOSTROPHE = '\"';
	private static final char ESCAPE_CHAR = '\\';
	private static final char SLASH_CHAR = '/';
	private static final char HASH_CHAR = '#';
	private static final char DASH_CHAR = '-';
	private static final char STAR_CHAR = '*';
	private static final char EXCLAMATION_CHAR = '!';
	protected ResourceLoader resourceLoader;
	protected String encoding = "UTF-8";
	protected String resourcePath = "classpath:/META-INF/darwin/sql/";

	/**
	 * Constructor.
	 */
	public DefaultResourceAccessor() {}

	/**
	 * Constructor.
	 *
	 * @param resourceLoader Spring {@link ResourceLoader} to use for loading resources
	 * @param encoding encoding of the resources
	 * @param resourcePath base path where patches should be looked up for
	 */
	public DefaultResourceAccessor(ResourceLoader resourceLoader, String encoding, String resourcePath) {
		this.resourceLoader = resourceLoader;
		this.encoding = encoding;
		this.resourcePath = resourcePath;
	}

	/**
	 * Returns resource list (means list of resource objects) ordered by name for certain platform.
	 *
	 * @param platform
	 */
	@Override
	public Resource[] getSortedResourceList(Platform platform) {
		final String normalizedPath = normalizePath(this.resourcePath, platform.getFolderName(), true);
		final PathMatchingResourcePatternResolver resolver = new OC4JPathMatchingResourcePatternResolver(resourceLoader);
		return getResources(normalizedPath, resolver);
	}

	/**
	 * Returns tokenized SQL statements in list.
	 */
	@Override
	public List<String> getTokenizedSQLScriptContentFromResource(String resourcePath) {
		final String content = getTextContentFromResource(resourcePath);
		return tokenizeSQLScriptContent(content);
	}

	/**
	 * Returns unparsed text content of specified resource.
	 */
	@Override
	public String getTextContentFromResource(String resourcePath) {
		String normalizedPath = normalizePath(this.resourcePath, resourcePath, false);
		//base path may contain fe: classpath*:/directory ... so when looking up for specific resource, asterisk must be removed
		normalizedPath = normalizedPath.replaceAll("\\*", "");

		final Resource resource = resourceLoader.getResource(normalizedPath);
		return readResource(resourcePath, normalizedPath, resource);
	}

	/**
	 * Tokenizes content by specified delimiter and puts it into list.
	 */
	protected List<String> tokenizeSQLScriptContent(String content) {
		Assert.notNull(content, "SQL content is NULL!");
		final List<String> result = new LinkedList<String>();
		final int contentLength = content.length();

		boolean inString = false;
		int stringStartIndex = -1;
		boolean inComment = false;
		boolean lineComment = false;
		final StringBuilder buffer = new StringBuilder();

		for(int i = 0; i < contentLength; i++) {
			char currentChar = content.charAt(i);

			if(!inString && isLineComment(content, i, currentChar)) {
				lineComment = true;
				inComment = true;
				i++;
			} else if(currentChar == '\n' && lineComment) {
				lineComment = false;
				inComment = false;
			} else if(!inString && isOpeningBlockComment(content, i, currentChar)) {
				inComment = true;
				i++;
			} else if(inComment && !lineComment &&
					isClosingBlockComment(content, i, currentChar)) {
				inComment = false;
				i++;
			} else if(!inComment &&
					(isStringDelimiter(content, i, stringStartIndex, currentChar, SINGLE_APOSTROPHE) ||
							isStringDelimiter(content, i, stringStartIndex, currentChar, DOUBLE_APOSTROPHE))) {
				inString = !inString;
				stringStartIndex = inString ? i : -1;
				buffer.append(currentChar);
			} else if(currentChar == SEMICOLON && !inString && !inComment) {
				if(isLineDelimiter(content, i, currentChar, SEMICOLON)) {
					addQuery(result, buffer);
					buffer.setLength(0);
				} else {
					buffer.append(currentChar);
					i++;
				}
			} else if(!inComment) {
				buffer.append(currentChar);
			}
		}
		addQuery(result, buffer);
		return result;
	}

	/**
	 * Automatically finds all resources on path.
	 */
	protected Resource[] getResources(String normalizedPath, PathMatchingResourcePatternResolver resolver) {
		Resource[] resources = null;
		try {
			resources = resolver.getResources(normalizedPath + "*");
			Arrays.sort(resources, new ResourceVersionComparator());
		} catch(IOException ex) {
			resources = tryToFindResourceListInDescriptor(resolver, normalizedPath, resources);

			if(resources == null) {
				String msg = "Cannot get list of available resources from '" + normalizedPath
						+ "' for db autoupdate!";
				log.fatal(msg);
				throw new RuntimeException(msg, ex);
			}
		}
		return resources;
	}

	/**
	 * Reads contents of the resource.
	 */
	protected String readResource(String resourceName, String normalizedPath, Resource resource) {
		if(resource.exists()) {
			try (final InputStream is = resource.getInputStream()) {
				final String content = IOUtils.toString(is, encoding).trim();
				if(content.endsWith(";")) {
					return content.substring(0, content.length() - 1);
				}
				return content;
			} catch(IOException ex) {
				final String msg = "Unexpectedly cannot read resource: " + normalizedPath + resourceName;
				log.fatal(msg, ex);
				throw new RuntimeException(msg, ex);
			}
		} else {
			if(log.isWarnEnabled()) {
				log.warn("Can not find resource: " + normalizedPath);
			}
			return null;
		}
	}

	/**
	 * Attempts to find and parse descriptor.txt file on classpath and retrieve list of patches from it.
	 */
	protected Resource[] tryToFindResourceListInDescriptor(PathMatchingResourcePatternResolver resolver,
														   String normalizedPath, Resource[] resources) {
		//try to locate descriptor file
		final Resource resource = resolver.getResource(normalizedPath + DESCRIPTOR_FILE);
		if(resource.exists()) {
			try (final InputStream is = resource.getInputStream()) {
				final String list = IOUtils.toString(is, encoding);
				final List<Resource> result = new LinkedList<>();

				for(StringTokenizer st = new StringTokenizer(list, "\n", false); st.hasMoreTokens(); ) {
					final String item = st.nextToken();
					final Resource res = resolver.getResource(item.trim());
					if(res.exists()) {
						result.add(res);
					} else {
						if(log.isWarnEnabled()) {
							log.warn("Descriptor contains reference to resource: " + res.toString() +
									" but it does not exist!");
						}
					}
				}

				resources = new Resource[result.size()];
				for(int i = 0; i < result.size(); i++) {
					final Resource res = result.get(i);
					resources[i] = res;
				}

			} catch(IOException ex) {
				final String msg = "Cannot open descriptor resource at path: " + normalizedPath + DESCRIPTOR_FILE;
				log.error(msg);
				throw new RuntimeException(msg, ex);
			}
		}
		return resources;
	}

	/**
	 * Normalizes path.
	 */
	protected String normalizePath(String resourcePath, String resourceName, boolean directory) {
		if(!resourcePath.endsWith("/")) {
			resourcePath = resourcePath + "/";
		}
		if(resourceName.startsWith("/")) {
			resourceName = resourceName.substring(1);
		}
		final String finalName = resourcePath + resourceName;
		return directory && !finalName.endsWith("/") ? finalName + "/" : finalName;
	}

	private boolean isLineComment(String content, int currentPos, char currentChar) {
		char nextChar = currentPos + 1 < content.length() ? content.charAt(currentPos + 1) : '-';
		return currentChar == HASH_CHAR || (currentChar == DASH_CHAR && nextChar == DASH_CHAR);
	}

	private boolean isOpeningBlockComment(String content, int currentPos, char currentChar) {
		char nextChar = currentPos + 1 < content.length() ? content.charAt(currentPos + 1) : '-';
		char nextNextChar = currentPos + 2 < content.length() ? content.charAt(currentPos + 2) : '-';
		return currentChar == SLASH_CHAR && nextChar == STAR_CHAR && nextNextChar != EXCLAMATION_CHAR;
	}

	private boolean isClosingBlockComment(String content, int currentPos, char currentChar) {
		char nextChar = currentPos + 1 < content.length() ? content.charAt(currentPos + 1) : '-';
		return currentChar == STAR_CHAR && nextChar == SLASH_CHAR;
	}

	private void addQuery(List<String> result, StringBuilder buffer) {
		String query = buffer.toString().trim();
		if(query.length() > 0 && !(query.length() == 1 && query.charAt(0) == SEMICOLON)) {
			result.add(query);
		}
	}

	private boolean isStringDelimiter(String content, int currentPos, int stringStartIndex, char currentChar,
									  char delimiterChar) {
		char prevChar = currentPos - 1 >= 0 ? content.charAt(currentPos - 1) : '-';
		char nextChar = currentPos + 1 < content.length() ? content.charAt(currentPos + 1) : '-';
		boolean isDelimiter = currentChar == delimiterChar;
		boolean isNotEscaped = prevChar != ESCAPE_CHAR;
		boolean isNotDuplicatedAndInsideString = (prevChar != delimiterChar || stringStartIndex == currentPos - 1) &&
				nextChar != delimiterChar;
		boolean isTriplecatedInsideString = prevChar == delimiterChar && nextChar == delimiterChar;
		return isDelimiter && isNotEscaped && (isNotDuplicatedAndInsideString || isTriplecatedInsideString);
	}

	private boolean isLineDelimiter(String content, int currentPos, char currentChar, char delimiterChar) {
		char nextChar = currentPos + 1 < content.length() ? content.charAt(currentPos + 1) : '-';
		return currentChar == delimiterChar && nextChar != delimiterChar;
	}

	/**
	 * TODO JNO ... tohle možná odebrat?
	 */
	protected static class OC4JPathMatchingResourcePatternResolver extends PathMatchingResourcePatternResolver {

		/**
		 * Create a new PathMatchingResourcePatternResolver with a DefaultResourceLoader.
		 * <p>ClassLoader access will happen via the thread context class loader.
		 *
		 * @see org.springframework.core.io.DefaultResourceLoader
		 */
		OC4JPathMatchingResourcePatternResolver() {
			super();
		}

		/**
		 * Create a new PathMatchingResourcePatternResolver with a DefaultResourceLoader.
		 *
		 * @param classLoader the ClassLoader to load classpath resources with,
		 *                    or <code>null</code> for using the thread context class loader
		 * @see org.springframework.core.io.DefaultResourceLoader
		 */
		OC4JPathMatchingResourcePatternResolver(ClassLoader classLoader) {
			super(classLoader);
		}

		/**
		 * Create a new PathMatchingResourcePatternResolver.
		 * <p>ClassLoader access will happen via the thread context class loader.
		 *
		 * @param resourceLoader the ResourceLoader to load root directories and
		 *                       actual resources with
		 */
		OC4JPathMatchingResourcePatternResolver(ResourceLoader resourceLoader) {
			super(resourceLoader);
		}

		@Override
		protected boolean isJarResource(Resource resource) throws IOException {
			/**
			 * SUPPORT FOR CPS RESOURCES - NOT REQUIRING TO HAVE CPS ON CLASSPATH
			 */
			if("com.fg.webapp.cps.v1.modules.spring.resource.StorageResource".equals(resource.getClass().getName())) {
				return false;
			}
			if("com.fg.webapp.cps.v1.modules.spring.resource.TemplateResource".equals(resource.getClass().getName())) {
				return false;
			}
			URL url = resource.getURL();
			//hack for OC4J 10.1.3.X
			if(url.getProtocol().equals("code-source") && url.getPath().contains(".jar!")) return true;
			//for other rationally behaving servers
			return super.isJarResource(resource);
		}
	}

}
