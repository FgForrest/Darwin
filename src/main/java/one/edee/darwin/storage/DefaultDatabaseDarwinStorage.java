package one.edee.darwin.storage;

import lombok.*;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.SqlCommand;
import one.edee.darwin.model.version.VersionDescriptor;
import one.edee.darwin.resources.ResourceMatcher;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.lang.Nullable;
import org.springframework.util.DigestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Optional.ofNullable;

/**
 * Default implementation of the Darwin storage.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@SuppressWarnings("SqlSourceToSinkFlow")
@CommonsLog
public class DefaultDatabaseDarwinStorage extends AbstractDatabaseStorage implements DarwinStorage {
    private static final Map<StatementTypeWithPlatform, String> STATEMENTS_CACHE = new ConcurrentHashMap<>();
    @Getter
    private final ResourceMatcher resourceMatcher;
    @Getter
    private final StorageChecker storageChecker;
    @Getter
    @Setter
    private boolean patchAndTableExists;

    public DefaultDatabaseDarwinStorage(
        @NonNull ResourceMatcher resourceMatcher,
        @NonNull StorageChecker storageChecker
    ) {
        this.resourceMatcher = resourceMatcher;
        this.storageChecker = storageChecker;
    }

    @Nullable
    @Override
    public VersionDescriptor getVersionDescriptorForComponent(@NonNull String componentName) {
        final String versionScript = STATEMENTS_CACHE.computeIfAbsent(
            getKey(StorageStatement.GET_VERSION), this::readContentFromResource
        );
        try {
            String version = jdbcTemplate.queryForObject(versionScript, String.class, componentName);
            if (version != null && !version.trim().isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Detected version of component " + componentName + " is " + version);
                }
                return new VersionDescriptor(version.trim());
            } else {
                return null;
            }
        } catch (EmptyResultDataAccessException | BadSqlGrammarException ignored) {
            //EmptyResultDataAccessException might happen when there are no record in Darwin
            //BadSqlGrammarException might happen when there are no infrastructural tables of Darwin
            if (log.isDebugEnabled()) {
                log.debug("No version of component " + componentName + " detected.");
            }
            return null;
        }

    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    @Override
    public void updateVersionDescriptorForComponent(@NonNull String componentName, @NonNull String version) {
        final VersionDescriptor storedVersion = getVersionDescriptorForComponent(componentName);
        if (storedVersion == null) {
            final String insertScript = STATEMENTS_CACHE.computeIfAbsent(
                getKey(StorageStatement.INSERT_COMPONENT), this::readContentFromResource
            );
            jdbcTemplate.update(insertScript, componentName, version);
        } else {
            final String updateScript = STATEMENTS_CACHE.computeIfAbsent(
                getKey(StorageStatement.UPDATE_COMPONENT), this::readContentFromResource
            );
            jdbcTemplate.update(updateScript, version, componentName);
        }
    }

    @Override
    public void insertSqlScriptToDB(@NonNull Patch patch, @NonNull SqlCommand sqlCommand) {
        insertSqlScriptToDB(
            patch, sqlCommand.getStatement(),
            sqlCommand.getProcessTime(), sqlCommand.getFinishedOn(),
            sqlCommand.getException()
        );
    }

    @Override
    public void updateSqlScriptInDB(@NonNull Patch patch, @NonNull SqlCommand sqlCommand) {
        updateSqlScriptInDB(
            patch, sqlCommand.getStatement(),
            sqlCommand.getProcessTime(), sqlCommand.getFinishedOn(),
            sqlCommand.getException()
        );
    }

    @SuppressWarnings("SqlSourceToSinkFlow")
    @Override
    public Patch insertPatchToDatabase(
        @NonNull String patchName,
        @NonNull String componentName,
        @NonNull LocalDateTime detectedOn,
        @NonNull Platform platform
    ) {
        String sqlCommandWhichThrewException = "";
        try {
            //tries to insert information about patch to the database - might fail on duplicate key exception
            //if there already is some patch of this name for this component
            final String sql = STATEMENTS_CACHE.computeIfAbsent(
                getKey(StorageStatement.INSERT_PATCH), this::readContentFromResource
            );
            sqlCommandWhichThrewException = sql;
            jdbcTemplate.update(sql, componentName, patchName, 0, java.sql.Timestamp.valueOf(detectedOn), null, platform.name());

            //insert was ok - we need to load patch from db
            return getPatchFromDb(patchName, componentName, platform);

        } catch (DuplicateKeyException ignored) {

            //patch is already present in database - just select its ID, this is expected situation
            return getPatchFromDb(patchName, componentName, platform);

        } catch (DataIntegrityViolationException ex) {

            final VersionDescriptor component = getVersionDescriptorForComponent(componentName);
            if (component == null) {
                log.debug("Component information not present in database - creating new record for " + componentName + ".");
                //no component exists in database yet - create it and retry
                insertComponentToDatabase(componentName);
                return insertPatchToDatabase(patchName, componentName, detectedOn, platform);
            } else {
                log.error("Can't insert patch into the database: " + ex.getMessage(), ex);
                throw ex;
            }

        } catch (DataAccessException ex) {

            log.error("Failed to execute script!!!" + sqlCommandWhichThrewException, ex);
            throw new RuntimeException(ex);

        }
    }

    @Override
    public void markPatchAsFinished(@NonNull Patch patch) {
        if (patch.isInDb()) {
            final String sql = STATEMENTS_CACHE.computeIfAbsent(
                getKey(StorageStatement.MARK_PATCH_AS_FINISHED), this::readContentFromResource
            );
            jdbcTemplate.update(
                sql,
                java.sql.Timestamp.valueOf(LocalDateTime.now()),
                patch.getPatchName(), patch.getComponentName(), patch.getPlatform().name()
            );
        }
    }

    @NonNull
    @Override
    public Patch getPatchByResourcePath(@NonNull String resourcePath, @NonNull String componentName) {
        final String[] strings = resourceMatcher.getPlatformAndNameFromResourcePath(resourcePath);
        if (storageChecker.existPatchAndSqlTable()) {
            final Patch patchFromDb = getPatchFromDb(strings[1], componentName, Platform.identify(strings[0]));
            if (patchFromDb == null) {
                return insertPatchToDatabase(strings[1], componentName, LocalDateTime.now(), Platform.identify(strings[0]));
            } else {
                return patchFromDb;
            }
        } else {
            return new Patch(componentName, strings[1], Platform.identify(strings[0]), LocalDateTime.now());
        }
    }

    @Override
    public boolean isPatchRecordedByResourcePath(@NonNull String resourcePath, @NonNull String componentName) {
        final String[] strings = resourceMatcher.getPlatformAndNameFromResourcePath(resourcePath);
        if (!storageChecker.existPatchAndSqlTable()) {
            return false;
        }
        final Patch patchFromDb = getPatchFromDb(strings[1], componentName, Platform.identify(strings[0]));
        return patchFromDb != null;
    }

    @Override
    public boolean isPatchFinishedInDb(@NonNull Patch patch) {
        final Patch patchFromDb;
        try {
            patchFromDb = getPatchFromDb(patch.getPatchName(), patch.getComponentName(), patch.getPlatform());
        } catch (BadSqlGrammarException ignored) {
            // DARWiN_PATCH table probably doesn't exist.
            return false;
        }
        return patchFromDb != null && patchFromDb.getFinishedOn() != null;
    }

    @Override
    public boolean isAnyPatchRecordedFor(@NonNull String componentName) {
        final String sql = STATEMENTS_CACHE.computeIfAbsent(
            getKey(StorageStatement.IS_ANY_PATCH_FOR_COMPONENT), this::readContentFromResource
        );
        int countOfPatchesInDb = jdbcTemplate.queryForList(sql, componentName).size();
        return countOfPatchesInDb != 0;
    }

    @Override
    public void insertComponentToDatabase(@NonNull String componentName) {
        final String sql = STATEMENTS_CACHE.computeIfAbsent(
            getKey(StorageStatement.INSERT_COMPONENT), this::readContentFromResource
        );
        jdbcTemplate.update(sql, componentName, "1.0");
    }

    @NonNull
    @Override
    public SqlScriptStatus wasSqlCommandAlreadyExecuted(int patchId, @NonNull String script, int occurrence) {
        if (storageChecker.existPatchAndSqlTable()) {
            final String sql = STATEMENTS_CACHE.computeIfAbsent(
                getKey(StorageStatement.WAS_SQL_EXECUTED), this::readContentFromResource
            );
            final List<Date> finishedDates = jdbcTemplate.queryForList(
                sql,
                Date.class,
                patchId,
                occurrence > 1 ? "-- occurrence: " + occurrence + "\n" + script : script
            );
            if (!finishedDates.isEmpty()) {
                boolean atLeastOneFinished = false;
                for (Date executedDate : finishedDates) {
                    if (executedDate != null) {
                        atLeastOneFinished = true;
                        break;
                    }
                }
                return atLeastOneFinished ? SqlScriptStatus.EXECUTED_FINISHED : SqlScriptStatus.EXECUTED_FAILED;
            }
        }
        return SqlScriptStatus.NOT_EXECUTED;
    }

    private void insertSqlScriptToDB(
        @NonNull Patch patch,
        @NonNull String statement,
        long processTime,
        @NonNull LocalDateTime finishedOn,
        @Nullable Exception exception
    ) {
        if (patch.isInDb()) {
            final String sql = STATEMENTS_CACHE.computeIfAbsent(
                getKey(StorageStatement.INSERT_SQL_SCRIPT), this::readContentFromResource
            );
            jdbcTemplate.update(
                sql, patch.getPatchId(), statement, computeHash(statement),
                processTime,
                java.sql.Timestamp.valueOf(finishedOn),
                exception != null ? exceptionToString(exception) : null
            );
        }
    }

    private void updateSqlScriptInDB(
        @NonNull Patch patch,
        @NonNull String statement,
        long processTime,
        @NonNull LocalDateTime finishedOn,
        @Nullable Exception exception
    ) {
        if (patch.isInDb()) {
            final String sql = STATEMENTS_CACHE.computeIfAbsent(
                getKey(StorageStatement.UPDATE_SQL_SCRIPT), this::readContentFromResource
            );
            jdbcTemplate.update(
                sql,
                processTime,
                java.sql.Timestamp.valueOf(finishedOn),
                exception != null ? exceptionToString(exception) : null,
                patch.getPatchId(), computeHash(statement)
            );
        }
    }

    private String computeHash(@NonNull String statement) {
        return DigestUtils.md5DigestAsHex(statement.getBytes(StandardCharsets.UTF_8));
    }

    @Nullable
    private Patch getPatchFromDb(
        @NonNull String patchName,
        @NonNull String componentName,
        @NonNull Platform platform
    ) {
        try {
            final String sql = STATEMENTS_CACHE.computeIfAbsent(
                getKey(StorageStatement.GET_PATCH), this::readContentFromResource
            );
            return jdbcTemplate.queryForObject(
                sql,
                new Object[]{patchName, componentName, platform.name()},
                (rs, rowNum) -> new Patch(
                    rs.getInt("id"),
                    rs.getString("patchName"),
                    rs.getString("componentName"),
                    rs.getTimestamp("detectedOn").toLocalDateTime(),
                    Platform.identify(rs.getString("platform")),
                    ofNullable(rs.getTimestamp("finishedOn")).map(Timestamp::toLocalDateTime).orElse(null)
                ));
        } catch (EmptyResultDataAccessException ignored) {
            //no patch exists in database
            return null;
        }
    }

    /**
     * Converts exception to string.
     */
    @NonNull
    private String exceptionToString(@NonNull Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    @NonNull
    private StatementTypeWithPlatform getKey(@NonNull StorageStatement statementType) {
        return new StatementTypeWithPlatform(statementType, getPlatform());
    }

    @NonNull
    private String readContentFromResource(@NonNull StatementTypeWithPlatform statementTypeWithPlatform) {
        return dbResourceAccessor.getTextContentFromResource(
            statementTypeWithPlatform.getPlatform().getFolderName() + "/" +
                statementTypeWithPlatform.getStatementType().getFileName()
        );
    }

    @RequiredArgsConstructor
    private enum StorageStatement {
        INSERT_COMPONENT("insert_component.sql"),
        UPDATE_COMPONENT("update_component.sql"),
        IS_ANY_PATCH_FOR_COMPONENT("select_isAnyPatchRecordedForComponent.sql"),
        GET_PATCH("select_patchFromDb.sql"),
        INSERT_PATCH("insert_patch.sql"),
        MARK_PATCH_AS_FINISHED("update_markPatchAsFinished.sql"),
        UPDATE_SQL_SCRIPT("update_script.sql"),
        GET_VERSION("version.sql"),
        WAS_SQL_EXECUTED("select_wasSqlCommandAlreadyExecuted.sql"),
        INSERT_SQL_SCRIPT("insert_script.sql");

        @Getter
        private final String fileName;
    }

    @Data
    private static class StatementTypeWithPlatform {
        private final StorageStatement statementType;
        private final Platform platform;
    }

}
