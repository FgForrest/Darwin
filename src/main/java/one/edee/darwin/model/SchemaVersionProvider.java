package one.edee.darwin.model;

/**
 * Basic version accessor interface. It serves to identify component name and version that is coming up in the application
 * setup and to compare with version that is already deployed in database.
 *
 * Although it was usually connected with Maven POM version it is discouraged now (after experience with such doing).
 * It's better to version data model independently to application version and update version only when patch is added.
 * It allows you to deploy several patches in single application version and this is better for iterative development
 * practises.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface SchemaVersionProvider {

	/**
	 * Returns component name.
	 */
	String getComponentName();

	/**
	 * Returns specific version of data library.
	 */
	String getComponentVersion();

}
