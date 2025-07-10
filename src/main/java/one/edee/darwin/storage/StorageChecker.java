package one.edee.darwin.storage;

import lombok.NonNull;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.version.VersionDescriptor;
import org.springframework.lang.Nullable;

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
	@NonNull
	Platform getPlatform();

    /**
     * Executes script that allows to guess existing version of the data layer.
     *
     * @param componentName unique name of the component
     * @param darwinStorage storage implementation
     */
	@Nullable
    VersionDescriptor guessVersion(
		@NonNull String componentName,
		@NonNull DarwinStorage darwinStorage
	);

	/**
	 * Executes script that allows to guess if particular patch is already present in database.
	 *
	 * @param componentName unique name of the component
	 * @param darwinStorage storage implementation
	 * @param checkedVersion version identification that is required to be detected from the current DB schema
	 * @return true if checkedVersion is already present in the database
	 */
	boolean guessPatchAlreadyApplied(
		@NonNull String componentName,
		@NonNull DarwinStorage darwinStorage,
		@NonNull VersionDescriptor checkedVersion
	);

    /**
     * Looks at table schema and return true if tables DARWIN_PATCH and DARWIN_SQL exist
     *
     * @return true if tables exists
     */
    boolean existPatchAndSqlTable();

}
