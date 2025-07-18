package one.edee.darwin.storage;

import lombok.NonNull;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.SqlCommand;
import one.edee.darwin.model.version.VersionDescriptor;
import one.edee.darwin.resources.ResourceAccessor;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

/**
 * Describes base persistence layer contract for Darwin.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface DarwinStorage {

    /**
     * Contains all possible states of the SQL script.
     */
    enum SqlScriptStatus {

        EXECUTED_FAILED, EXECUTED_FINISHED, NOT_EXECUTED

    }

    /**
     * Sets initialized resource utils.
     *
     * @param resourceAccessor to use for accessing resources
     */
    void setResourceAccessor(@NonNull ResourceAccessor resourceAccessor);

    /**
     * Returns version descriptor for particular component.
     *
     * @param componentName unique identification of the component to retrieve version for.
     */
    @Nullable
    VersionDescriptor getVersionDescriptorForComponent(@NonNull String componentName);

    /**
     * Updates version descriptor for particular component.
     *
     * @param componentName unique identification of the component for updating version
     * @param version of the component
     */
    void updateVersionDescriptorForComponent(@NonNull String componentName, @NonNull String version);

    /**
     * Inserts patch to db and returns its ID. If patch is already in db it returns ID of such existing patch.
     *
     * @param patchName file name of the patch
     * @param componentName unique identification of the component this patch belongs to
     * @param detectedOn date and time patch was observed for the first time
     * @param platform target database platform of the patch
     * @return patch identification
     */
    Patch insertPatchToDatabase(
        @NonNull String patchName,
        @NonNull String componentName,
        @NonNull LocalDateTime detectedOn,
        @NonNull Platform platform
    );

    /**
     * Inserts information about performed SQL command to database.
     *
     * @param patch file name of the patch
     * @param sqlCommand contents of the SQL command inside the patch
     */
    void insertSqlScriptToDB(@NonNull Patch patch, @NonNull SqlCommand sqlCommand);

    /**
     * Updates information about performed SQL command in database.
     *
     * @param patch file name of the patch
     * @param sqlCommand contents of the SQL command inside the patch
     */
    void updateSqlScriptInDB(@NonNull Patch patch, @NonNull SqlCommand sqlCommand);

    /**
     * Stores timestamp of the completely and <b>successfully</b> applied patch.
     *
     * @param patch identification
     */
    void markPatchAsFinished(@NonNull Patch patch);

    /**
     * Returns record for the patch by its path and component name in case it exists in database.
     *
     * @param resourcePath  platform and name of patch, example mysql/patch_1.1.sql
     * @param componentName name of component on which is patch applied
     * @return patch from db, by #resourcePath and #componentName
     */
    @NonNull
    Patch getPatchByResourcePath(@NonNull String resourcePath, @NonNull String componentName);

    /**
     * Tries to find record for the patch in internal database and looks for timestamp {@link Patch#getFinishedOn()} that
     * signalizes that patch was successfully and entirely applied.
     *
     * @param patch identification
     * @return true if patch was entirely applied
     */
    boolean isPatchFinishedInDb(@NonNull Patch patch);

    /**
     * Check if the patch by its path and component name in case it exists in database.
     *
     * @param resourcePath  platform and name of patch, example mysql/patch_1.1.sql
     * @param componentName name of component on which is patch applied
     * @return true if patch record is in database
     */
    boolean isPatchRecordedByResourcePath(@NonNull String resourcePath, @NonNull String componentName);

    /**
     * Returns true if there is any patch recorded for this component.
     * Determines whether the component was updated by the new generation of darwin.
     *
     * @param componentName name of component what we want check
     */
    boolean isAnyPatchRecordedFor(@NonNull String componentName);

    /**
     * Inserts information about the component to the database with initial data layer version of 1.0
     */
    void insertComponentToDatabase(@NonNull String componentName);

    /**
     * Checks if is sql command was already applied to the database.
     *
     * @param patchId id of the patch this script is part of
     * @param script {@link SqlCommand#getStatement()}
	 * @param occurrence number of the identical occurence of the same script in the patch
     */
    @NonNull
    SqlScriptStatus wasSqlCommandAlreadyExecuted(int patchId, @NonNull String script, int occurrence);

}
