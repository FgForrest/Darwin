package one.edee.darwin.resources;

import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Description
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public interface ResourceAccessor {

	/**
	 * Sets path to the folder where component patches are stored.
	 */
	void setResourcePath(String resourcePath);

	/**
	 * Returns list of resources matching database patches for certain resource name.
	 * Resources are sorted alphabetically by their names.
	 *
	 * @param resourceName ResourceAccessor know path where are patch stored {@link #setResourcePath(String)}, but now we must specify which resourceName is used.
	 * @return return sorted resources, base on alphabet.
	 */
	Resource[] getSortedResourceList(String resourceName);

	/**
	 * Parses SQL script to individual SQL commands.
	 *
	 * @param resourceName name of patch which we want separated
	 * @return List of sql commands which are present in patch.
	 */
	List<String> getTokenizedSQLScriptContentFromResource(String resourceName);

	/**
	 * Returns string contents of the resource by its name.
	 *
	 * @param resourceName name of resource from witch we want text content
	 * @return Returns unparsed text content of specified resource.
	 */
	String getTextContentFromResource(String resourceName);

}
