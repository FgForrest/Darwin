package one.edee.darwin.model;


import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.exception.PatchFormatException;

import java.time.LocalDateTime;


/**
 * This class represent database patch - its id, component it relates to, date and time it was detected for the first
 * time in the application and database platform it relates to.
 *
 * @author Radek Salay, FG Forest a.s. 6/15/16.
 */
@Data
@CommonsLog
public class Patch {
    private final Integer patchId;
    private final String patchName;
    private final String componentName;
    private final LocalDateTime detectedOn;
    private final Platform platform;

    private int processTime;
    private LocalDateTime finishedOn;

    /**
     * @param patchName     name of current patch, for example 1.0.4
     * @param componentName id for which component patch is
     * @param detectedOn    time when was patch found on classpath
     * @param platform      for what platform patch is, for example MySQL
     */
    public Patch(Integer patchId, String patchName, String componentName,
                 LocalDateTime detectedOn, Platform platform) {
        validatePatch(patchId, patchName, componentName, detectedOn, platform);
        this.patchId = patchId;
        this.patchName = patchName;
        this.componentName = componentName;
        this.detectedOn = detectedOn;
        this.platform = platform;
    }

    /**
     * @param patchName     name of current patch, for example 1.0.4
     * @param componentName id for which component patch is
     * @param detectedOn    time when was patch found on classpath
     * @param platform      for what platform patch is, for example MySQL
     * @param finishedOn    date and time patch was successfully applied to database
     */
    public Patch(Integer patchId, String patchName, String componentName,
                 LocalDateTime detectedOn, Platform platform, LocalDateTime finishedOn) {
        validatePatch(patchId, patchName, componentName, detectedOn, platform);
        this.patchId = patchId;
        this.patchName = patchName;
        this.componentName = componentName;
        this.detectedOn = detectedOn;
        this.platform = platform;
        this.finishedOn = finishedOn;
    }

    /**
     * @param patchName     name of current patch, for example 1.0.4
     * @param componentName id for which component patch is
     * @param platform      for what platform patch is, for example MySQL
     * @param detectedOn    time when was patch found on classpath
     */
    public Patch(String patchName, String componentName, Platform platform, LocalDateTime detectedOn) {
        this.patchId = null;
        this.patchName = patchName;
        this.componentName = componentName;
        this.platform = platform;
        this.detectedOn = detectedOn;
    }

    /**
     * Returns true if information about the patch is already present in database.
     * @return
     */
    public boolean isInDb() {
        return this.patchId != null;
    }

    /**
     * Returns path to the patch contents.
     * @return
     */
    public String getResourcesPath() {
        return getPlatform().getFolderName() + "/" + getPatchName();
    }

    /**
     * Validates consistency of the patch.
     */
    private void validatePatch(Integer patchId, String patchName, String componentName,
                               LocalDateTime detectedOn, Platform platform) throws PatchFormatException {
        if (patchName.isEmpty()) {
            throw new PatchFormatException("PatchName must not be empty! patchId: " +
                    patchId + " patchName: " + patchName + " componentName: " +
                    componentName + " detectedOn: " + detectedOn + " platform: " + platform);
        }
        if (componentName.isEmpty()) {
            throw new PatchFormatException("Component name must not empty!  patchId: " +
                    patchId + " patchName: " + patchName + " componentName: " +
                    componentName + " detectedOn: " + detectedOn + " platform: " + platform);
        }
        if (detectedOn == null)
            throw new PatchFormatException("Date can not be null! patchId: " + patchId +
                    " patchName: " + patchName + " componentName: " + componentName +
                    " platform: " + platform);

        if (platform == null)
            throw new PatchFormatException("Patch must be applied on some platform! patchId: " +
                    patchId + " patchName: " + patchName + " componentName: " + componentName +
                    " detectedOn: " + detectedOn);
    }
}
