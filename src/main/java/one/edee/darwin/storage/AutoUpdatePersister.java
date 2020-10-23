package one.edee.darwin.storage;

import com.fg.commons.version.VersionDescriptor;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.SqlCommand;
import one.edee.darwin.resources.ResourceAccessor;

import java.util.Date;

/**
 * Description
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public interface AutoUpdatePersister {

    enum SqlScriptStatus {

        EXECUTED_FAILED, EXECUTED_FINISHED, NOT_EXECUTED

    }

    /**
     * Sets initialized resource utils.
     */
    void setResourceAccessor(ResourceAccessor resourceAccessor);

    /**
     * Returns version descriptor for particular component.
     */
    VersionDescriptor getVersionDescriptorForComponent(String componentName);

    /**
     * Updates version descriptor for particular component.
     */
    void updateVersionDescriptorForComponent(String componentName, String version);


    /**
     * Inserts patch to db and returns its ID.
     * If patch is already in db it returns ID of such existing patch.
     */
    Patch insertPatchToDatabase(final String patchName, final String componentName,
                                final Date detectedOn, final String platform);

    /**
     * Insert information about performed SQL command to database.
     */
    void insertSqlScriptToDB(Patch patch, SqlCommand sqlCommand);

    /**
     * Updates information about performed SQL command in database.
     * @param patch
     * @param sqlCommand
     */
    void updateSqlScriptInDB(Patch patch, SqlCommand sqlCommand);

    /**
     * Stores timestamp of the completely and <b>successfully</b> applied patch.
     */
    void markPatchAsFinished(Patch patch);

    /**
     * Returns record for the patch by its path and component name in case it exists in database.
     *
     * @param resourcePath  platform and name of patch, example mysql/patch_1.1.sql
     * @param componentName name of component on which is patch applied
     * @return patch from db, by #resourcePath and #componentName
     */
    Patch getPatchByResourcePath(String resourcePath, String componentName);

    /**
     * Tries to find record for the patch in internal database and looks for timestamp {@link Patch#finishedOn} that
     * signalizes that patch was successfully and entirely applied.
     *
     * @return true if patch was entirely applied
     */
    boolean isPatchFinishedInDb(Patch patch);

    /**
     * Check if the patch by its path and component name in case it exists in database.
     *
     * @param resourcePath  platform and name of patch, example mysql/patch_1.1.sql
     * @param componentName name of component on which is patch applied
     * @return true if patch record is in database
     */
    boolean isPatchRecordedByResourcePath(String resourcePath, String componentName);

    /**
     * Returns true if there is any patch recorded for this component.
     * Determines whether the component was updated by the new generation of autoupdater.
     *
     * @param componentName name of component what we want check
     */
    boolean isAnyPatchRecordedFor(String componentName);

    /**
     * Inserts information about the component to the database with initial data layer version of 1.0
     */
    void insertComponentToDatabase(String componentName);

    /**
     * Checks if is sql command was already applied to the database.
     *
     * @param patchId id of the patch this script is part of
     * @param script {@link SqlCommand#statement}
	 * @param occurrence number of the identical occurence of the same script in the patch
     */
    SqlScriptStatus wasSqlCommandAlreadyExecuted(int patchId, String script, int occurrence);

}
