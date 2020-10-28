package one.edee.darwin.resources;

import one.edee.darwin.model.Platform;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Interface provides access to patch storage. Allows to detect patches and access their contents.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface ResourceAccessor {

	/**
	 * Returns list of resources matching database patches for certain resource name.
	 * Resources are sorted alphabetically by their names.
	 *
	 * @param platform ResourceAccessors know path where patches are stored
	 * @return return sorted resources, base on alphabet.
	 */
	Resource[] getSortedResourceList(Platform platform);

	/**
	 * Parses SQL script to individual SQL commands.
	 *
	 * @param resourcePath name of patch which we want separated
	 * @return List of sql commands which are present in patch.
	 */
	List<String> getTokenizedSQLScriptContentFromResource(String resourcePath);

	/**
	 * Returns string contents of the resource by its name.
	 *
	 * @param resourcePath name of resource from witch we want text content
	 * @return Returns un-parsed text content of specified resource.
	 */
	String getTextContentFromResource(String resourcePath);

}
