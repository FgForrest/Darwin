package one.edee.darwin.storage;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.SqlCommand;
import one.edee.darwin.model.version.VersionDescriptor;
import one.edee.darwin.resources.ResourceNameAnalyzer;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * Default implementation of the Darwin storage.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@CommonsLog
public class DefaultDatabaseDarwinStorage extends AbstractDatabaseStorage implements DarwinStorage {
	@Getter private final ResourceNameAnalyzer resourceNameAnalyzer;
	@Getter private final StorageChecker storageChecker;
    @Getter @Setter private boolean patchAndTableExists;

	public DefaultDatabaseDarwinStorage(ResourceNameAnalyzer resourceNameAnalyzer, StorageChecker storageChecker) {
		Assert.notNull(resourceNameAnalyzer);
		Assert.notNull(storageChecker);
		this.resourceNameAnalyzer = resourceNameAnalyzer;
		this.storageChecker = storageChecker;
	}

	@Override
    public VersionDescriptor getVersionDescriptorForComponent(String componentName) {
        String versionScript = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/version.sql");
        try {
			String version = jdbcTemplate.queryForObject(versionScript, String.class, componentName);
			if(version != null && !version.trim().isEmpty()) {
				if(log.isDebugEnabled()) {
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

    @Override
    public void updateVersionDescriptorForComponent(String componentName, String version) {
        final VersionDescriptor storedVersion = getVersionDescriptorForComponent(componentName);
        if (storedVersion == null) {
            String insertScript = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/insert_component.sql");
            jdbcTemplate.update(insertScript, componentName, version);
        } else {
            String updateScript = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/update_component.sql");
            jdbcTemplate.update(updateScript, version, componentName);
        }
    }

    @Override
    public void insertSqlScriptToDB(Patch patch, SqlCommand sqlCommand) {
        insertSqlScriptToDB(
                patch, sqlCommand.getStatement(),
                sqlCommand.getProcessTime(), sqlCommand.getFinishedOn(),
                sqlCommand.getException()
        );
    }

	@Override
	public void updateSqlScriptInDB(Patch patch, SqlCommand sqlCommand) {
		updateSqlScriptInDB(
				patch, sqlCommand.getStatement(),
				sqlCommand.getProcessTime(), sqlCommand.getFinishedOn(),
				sqlCommand.getException()
		);
	}

    @Override
    public Patch insertPatchToDatabase(final String patchName, final String componentName, final LocalDateTime detectedOn, final Platform platform) {
        String sqlCommandWhichThrewException = "";
        try {
			//tries to insert information about patch to the database - might fail on duplicate key exception
            //if there already is some patch of this name for this component
            final String sql = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/insert_patch.sql");
            sqlCommandWhichThrewException = sql;
            jdbcTemplate.update(sql, componentName, patchName, 0, detectedOn, null, platform.name());

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
    public void markPatchAsFinished(Patch patch) {
        if (patch.isInDb()) {
            final String sql = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/update_markPatchAsFinished.sql");
            jdbcTemplate.update(
                    sql,
                    LocalDateTime.now(),
                    patch.getPatchName(), patch.getComponentName(), patch.getPlatform().name()
            );
        }
    }

    @Override
    public Patch getPatchByResourcePath(String resourcePath, String componentName) {
        final String[] strings = resourceNameAnalyzer.getPlatformAndNameFromResourcePath(resourcePath);
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
	public boolean isPatchRecordedByResourcePath(String resourcePath, String componentName) {
		final String[] strings = resourceNameAnalyzer.getPlatformAndNameFromResourcePath(resourcePath);
		if (!storageChecker.existPatchAndSqlTable()) {
			return false;
		}
		final Patch patchFromDb = getPatchFromDb(strings[1], componentName, Platform.identify(strings[0]));
		return patchFromDb != null;
	}

	@Override
    public boolean isPatchFinishedInDb(Patch patch) {
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
    public boolean isAnyPatchRecordedFor(String componentName) {
        final String sql = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/select_isAnyPatchRecordedForComponent.sql");
        int countOfPatchesInDb = jdbcTemplate.queryForList(sql, componentName).size();
        return countOfPatchesInDb != 0;
    }

    @Override
    public void insertComponentToDatabase(String componentName) {
        final String sql = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/insert_component.sql");
        jdbcTemplate.update(sql, componentName, "1.0");
    }

    @Override
    public SqlScriptStatus wasSqlCommandAlreadyExecuted(int patchId, String script, int occurrence) {
		if (storageChecker.existPatchAndSqlTable()) {
			final String sql = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/select_wasSqlCommandAlreadyExecuted.sql");
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
				return atLeastOneFinished?SqlScriptStatus.EXECUTED_FINISHED:SqlScriptStatus.EXECUTED_FAILED;
			}
		}
		return SqlScriptStatus.NOT_EXECUTED;
    }

    private void insertSqlScriptToDB(Patch patch, String statement,
                                     long processTime, Date finishedOn, Exception exception) {
        if (patch.isInDb()) {
        	// skip - table has different structure
	        if ("darwin".equals(patch.getComponentName()) && "patch_3.1.sql".equals(patch.getPatchName())) {
		        return;
	        }
            String sql = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/insert_script.sql");
            jdbcTemplate.update(
                    sql, patch.getPatchId(), statement, computeHash(statement),
                    processTime, finishedOn,
                    exception != null ? exceptionToString(exception) : null
            );
        }
    }

	private void updateSqlScriptInDB(Patch patch, String statement,
	                                 long processTime, Date finishedOn, Exception exception) {
		if (patch.isInDb()) {
			// skip - table has different structure
			if ("darwin".equals(patch.getComponentName()) && "patch_3.1.sql".equals(patch.getPatchName())) {
				return;
			}
			String sql = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/update_script.sql");
			jdbcTemplate.update(
					sql,
					processTime, finishedOn,
					exception != null ? exceptionToString(exception) : null,
					patch.getPatchId(), computeHash(statement)
			);
		}
	}

	private String computeHash(String statement) {
		return DigestUtils.md5DigestAsHex(statement.getBytes(StandardCharsets.UTF_8));
	}

	private Patch getPatchFromDb(String patchName, String componentName, Platform platform) {
		try {
			final String sql = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/select_patchFromDb.sql");
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
    private String exceptionToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
