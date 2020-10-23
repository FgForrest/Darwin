package one.edee.darwin.model;


import lombok.Data;
import one.edee.darwin.exception.PatchFormatException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;


/**
 * This class represent Patch. Contain all necessary information about him.
 *
 * @author Radek Salay, FG Forest a.s. 6/15/16.
 */
@Data
public class Patch {
    private static final Log log = LogFactory.getLog(Patch.class);

    private final Integer patchId;
    private final String patchName;
    private final String componentName;
    private final Date detectedOn;
    private final String platform;

    private int processTime;
    private Date finishedOn;

    /**
     * @param patchName     name of current patch, for example 1.0.4
     * @param componentName id for which component patch is
     * @param detectedOn    time when was patch found on classpath
     * @param platform      for what platform patch is, for example MySQL
     */
    public Patch(Integer patchId, String patchName, String componentName,
                 Date detectedOn, String platform) {
        patchValidation(patchId, patchName, componentName, detectedOn, platform);
        this.patchId = patchId;
        this.patchName = patchName;
        this.componentName = componentName;
        this.detectedOn = detectedOn;
        this.platform = platform;
    }

    public Patch(Integer patchId, String patchName, String componentName,
                 Date detectedOn, String platform, Date finishedOn) {
        patchValidation(patchId, patchName, componentName, detectedOn, platform);
        this.patchId = patchId;
        this.patchName = patchName;
        this.componentName = componentName;
        this.detectedOn = detectedOn;
        this.platform = platform;
        this.finishedOn = finishedOn;
    }

    public Patch(String patchName, String componentName, String platform, Date detectedOn) {
        this.patchId = null;
        this.patchName = patchName;
        this.componentName = componentName;
        this.platform = platform;
        this.detectedOn = detectedOn;
    }

    public boolean isInDb() {
        return this.patchId != null;
    }

    public String getResourcesPath() {
        return getPlatform() + "/" + getPatchName();
    }

    private void patchValidation(Integer patchId, String patchName, String componentName,
                                 Date detectedOn, String platform) throws PatchFormatException {
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
