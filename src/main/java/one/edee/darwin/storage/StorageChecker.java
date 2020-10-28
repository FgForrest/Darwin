package one.edee.darwin.storage;

import one.edee.darwin.model.Platform;
import one.edee.darwin.model.version.VersionDescriptor;

/**
 * Contains method for checking storage version of the component when there is no record in Darwin
 * internal data storage.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface StorageChecker {

	/**
	 * Returns platform of the storage.
	 */
	Platform getPlatform();

    /**
     * Executes script that allows to guess existing version of the data layer.
     *
     * @param componentName unique name of the component
     * @param darwinStorage storage implementation
     */
    VersionDescriptor guessVersion(String componentName, DarwinStorage darwinStorage);

	/**
	 * Executes script that allows to guess if particular patch is already present in database.
	 *
	 * @param componentName unique name of the component
	 * @param darwinStorage storage implementation
	 * @param checkedVersion version identification that is required to be detected from the current DB schema
	 * @return true if checkedVersion is already present in the database
	 */
	boolean guessPatchAlreadyApplied(String componentName, DarwinStorage darwinStorage, VersionDescriptor checkedVersion);

    /**
     * Looks at table schema and return true if tables DARWIN_PATCH and DARWIN_SQL exist
     *
     * @return true if tables exists
     */
    boolean existPatchAndSqlTable();
    
}
