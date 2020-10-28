package one.edee.darwin.storage;

/**
 * Contains method for updating components storage.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface StorageUpdater {

    /**
     * Executes update script.
     */
    void executeScript(String resourcePath, String componentName,
                       DarwinStorage darwinStorage, StorageChecker storageChecker);

}
