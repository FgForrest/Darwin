package one.edee.darwin.storage;

import com.fg.commons.version.VersionDescriptor;

/**
 * Contains method for checking storage version of the component when there is no record in dbautoupdate
 * internal data storage.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public interface StorageChecker {

	/**
	 * Returns platform of the storage.
	 */
	String getPlatform();

    /**
     * Executes script that allows to guess existing version of the data layer.
     */
    VersionDescriptor guessVersion(String componentName, AutoUpdatePersister autoUpdatePersister);

	/**
	 * Executes script that allows to guess if particular patch is already present in database.
	 */
	boolean guessPatchAlreadyApplied(String componentName, AutoUpdatePersister autoUpdatePersister, VersionDescriptor checkedVersion);

    /**
     * Looks at table schema and return true if tables T_DB_AUTOUPADE_PATCH and T_DB_AUTOUPADE_SQL exist
     *
     * @return true if tables exists
     */
    boolean existPatchAndSqlTable();
}
