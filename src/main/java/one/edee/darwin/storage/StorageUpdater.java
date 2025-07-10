package one.edee.darwin.storage;

import lombok.NonNull;

/**
 * Contains method for updating components storage.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface StorageUpdater {

    /**
     * Executes a script for a specified component while interacting with storage systems.
     *
     * @param resourcePath the path to the resource that contains the script to be executed; must not be null
     * @param componentName the unique name of the component for which the script is executed; must not be null
     * @param darwinStorage the storage handler for managing database operations and versioning; must not be null
     * @param storageChecker the utility to validate storage version and schema compatibility; must not be null
     */
    void executeScript(
        @NonNull String resourcePath,
        @NonNull String componentName,
        @NonNull DarwinStorage darwinStorage,
        @NonNull StorageChecker storageChecker
    );

}
