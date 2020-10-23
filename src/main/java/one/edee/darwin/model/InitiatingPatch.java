package one.edee.darwin.model;

import one.edee.darwin.storage.AutoUpdatePersister;

import java.util.Date;

/**
 * This is Patch which work with DB. He automatic write him self to db, and take patchId from DB
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
    public InitiatingPatch(String patchName, String componentName, Date detectedOn,
                           String platform, AutoUpdatePersister autoUpdatePersister) {
        super(
                autoUpdatePersister.insertPatchToDatabase(patchName, componentName,
                        detectedOn, platform).getPatchId(),
                patchName,
                componentName,
                detectedOn,
                platform);
    }
}
