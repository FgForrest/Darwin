package one.edee.darwin.model;

import one.edee.darwin.storage.DarwinStorage;

import java.time.LocalDateTime;

/**
 * Specific type of the patch that is used to gather patch id from the database.
 *
 * @author Radek Salay, FG Forest a.s. 6/22/16.
 */
public class InitiatingPatch extends Patch {


    /**
     * @param patchName     name of current patch, for example patch_1.0.4
     * @param componentName id for which component patch is
     * @param detectedOn    time when was patch found on classpath
     * @param platform      for what platform patch is, for example MySQL
     */
    public InitiatingPatch(String patchName, String componentName, LocalDateTime detectedOn,
                           Platform platform, DarwinStorage darwinStorage) {
        super(
                darwinStorage.insertPatchToDatabase(
                        patchName, componentName,
                        detectedOn, platform
                ).getPatchId(),
                patchName,
                componentName,
                detectedOn,
                platform);
    }
}
