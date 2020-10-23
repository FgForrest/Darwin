package one.edee.darwin.resources;

import lombok.Data;
import one.edee.darwin.model.ResourceVersionComparator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Description
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
@Data
public class DefaultResourceAccessor implements ResourceLoaderAware, ResourceAccessor {
	private static final Log log = LogFactory.getLog(DefaultResourceAccessor.class);
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
	protected String resourcePath = "classpath:/META-INF/lib_db_autoupdate/sql/";

	public DefaultResourceAccessor() {}

	public DefaultResourceAccessor(ResourceLoader resourceLoader, String encoding, String resourcePath) {
		this.resourceLoader = resourceLoader;
		this.encoding = encoding;
		this.resourcePath = resourcePath;
	}

	/**
	 * Returns resource list (means list of resource objects) ordered by name.
	 */
	@Override
	public Resource[] getSortedResourceList(String resourceName) {
		String normalizedPath = normalizePath(this.resourcePath, resourceName, true);
		PathMatchingResourcePatternResolver resolver =
				new OC4JPathMatchingResourcePatternResolver(resourceLoader);

		return getResources(normalizedPath, resolver);
	}

	/**
	 * Returns tokenized items in list.
	 */
	@Override
	public List<String> getTokenizedSQLScriptContentFromResource(String resourceName) {
		String content = getTextContentFromResource(resourceName);
		return tokenizeSQLScriptContent(content);
	}

	/**
	 * Tokenizes content by specified delimiter and puts it into list.
	 */
	public List<String> tokenizeSQLScriptContent(String content) {
		Assert.notNull(content, "SQL content is NULL!");
		List<String> result = new LinkedList<String>();

		int contentLength = content.length();
		boolean inString = false;
		int stringStartIndex = -1;
		boolean inComment = false;
		boolean lineComment = false;
		StringBuilder buffer = new StringBuilder();

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
	 * Returns unparsed text content of specified resource.
	 */
	@Override
	public String getTextContentFromResource(String resourceName) {
		String normalizedPath = normalizePath(resourcePath, resourceName, false);
		//base path may contain fe: classpath*:/directory ... so when looking up for specific resource, asterisk must be removed
		normalizedPath = normalizedPath.replaceAll("\\*", "");
		Resource resource = resourceLoader.getResource(normalizedPath);

		return readResource(resourceName, normalizedPath, resource);
	}

	protected Resource[] getResources(String normalizedPath, PathMatchingResourcePatternResolver resolver) {
		Resource[] resources = null;
		try {
			resources = resolver.getResources(normalizedPath + "*");
			if(resources != null) {
				Arrays.sort(resources, new ResourceVersionComparator());
			} else {
				String msg = "Resource " + normalizedPath + " cannot be found!";
				log.error(msg);
				throw new RuntimeException(msg);
			}
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

	protected String readResource(String resourceName, String normalizedPath, Resource resource) {
		if(resource.exists()) {
			InputStream is = null;
			try {
				is = resource.getInputStream();
				String content = IOUtils.toString(is, encoding).trim();
				if(content.endsWith(";")) {
					return content.substring(0, content.length() - 1);
				}
				return content;
			} catch(IOException ex) {
				String msg = "Unexpectedly cannot read resource: " + normalizedPath + resourceName;
				log.fatal(msg, ex);
				throw new RuntimeException(msg, ex);
			} finally {
				IOUtils.closeQuietly(is);
			}
		} else {
			if(log.isWarnEnabled()) {
				log.warn("Can not find resource: " + normalizedPath);
			}
			return null;
		}
	}

	protected Resource[] tryToFindResourceListInDescriptor(PathMatchingResourcePatternResolver resolver,
														   String normalizedPath, Resource[] resources) {
		//try to locate descriptor file
		Resource resource = resolver.getResource(normalizedPath + DESCRIPTOR_FILE);
		if(resource.exists()) {
			InputStream is = null;
			try {
				is = resource.getInputStream();
				String list = IOUtils.toString(is);
				List result = new ArrayList();

				for(StringTokenizer st = new StringTokenizer(list, "\n", false); st.hasMoreTokens(); ) {
					String item = st.nextToken();
					Resource res = resolver.getResource(item.trim());
					if(res != null) {
						if(res.exists()) {
							result.add(res);
						} else {
							if(log.isWarnEnabled()) {
								log.warn("Descriptor contains reference to resource: " + res.toString() +
										" but it does not exist!");
							}
						}
					}
				}

				resources = new Resource[result.size()];
				for(int i = 0; i < result.size(); i++) {
					Resource res = (Resource)result.get(i);
					resources[i] = res;
				}

			} catch(IOException ex) {
				String msg = "Cannot open descriptor resource at path: " + normalizedPath + DESCRIPTOR_FILE;
				log.error(msg);
				throw new RuntimeException(msg, ex);
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
		return resources;
	}

	/**
	 * Normalizes path.
	 */
	protected String normalizePath(String resourcePath, String resourceName, boolean directory) {
		if(!resourcePath.endsWith("/")) resourcePath = resourcePath + "/";
		if(resourceName.startsWith("/")) resourceName = resourceName.substring(1);
		String finalName = resourcePath + resourceName;
		if(directory && !finalName.endsWith("/")) finalName = finalName + "/";
		return finalName;
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

	protected class OC4JPathMatchingResourcePatternResolver extends PathMatchingResourcePatternResolver {

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
