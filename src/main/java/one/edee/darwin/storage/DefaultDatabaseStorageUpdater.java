package one.edee.darwin.storage;

import one.edee.darwin.model.Patch;
import one.edee.darwin.model.SqlCommand;
import one.edee.darwin.storage.AutoUpdatePersister.SqlScriptStatus;
import one.edee.darwin.utils.StringProcessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default database storage updater.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public class DefaultDatabaseStorageUpdater extends AbstractDatabaseStorage implements StorageUpdater {
    private static final Log log = LogFactory.getLog(DefaultDatabaseStorageUpdater.class);
	private final StorageChecker storageChecker;

	public DefaultDatabaseStorageUpdater(StorageChecker storageChecker) {
		Assert.notNull(storageChecker);
		this.storageChecker = storageChecker;
	}

	@Override
    public void executeScript(final String resourcePath, final String componentName, final AutoUpdatePersister autoUpdatePersister, final StorageChecker storageChecker) {
        if (transactionManager != null) {
            //though DDL commands makes implicit commit - do this in transaction in order to make Spring
            //return always the same connection to the database to share session among SQL commands
            new TransactionTemplate(transactionManager).execute(
                    new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(TransactionStatus status) {
                            performUpdate(resourcePath, componentName, autoUpdatePersister);
                        }
                    }
            );
        } else {
            performUpdate(resourcePath, componentName, autoUpdatePersister);
        }
    }

    /**
     * Reads resource from resourcePath, parses it to individual SQL commands and executes one by one.
     * It checks on the fly whether single SQL command was already executed (against its internal evidence)
     * and if it was it skips it.
     *
     * @param resourcePath  path to concrete patch
     * @param componentName name of updated component
     */
    private void performUpdate(String resourcePath, String componentName, AutoUpdatePersister autoUpdatePersister) {
        final List<String> sqlCommands = resourceAccessor.getTokenizedSQLScriptContentFromResource(resourcePath);
        final Patch patch = autoUpdatePersister.getPatchByResourcePath(resourcePath, componentName);
		final boolean patchAndSqlTableExists = storageChecker.existPatchAndSqlTable();

		long start = System.currentTimeMillis();

		final Map<String, Integer> executedCommands = new HashMap<>(sqlCommands.size());
        for (String sqlCommand : sqlCommands) {
			final Integer occurence = executedCommands.get(sqlCommand);
			final Integer newOccurence = occurence == null ? 1 : occurence + 1;
			executedCommands.put(sqlCommand, newOccurence);
	        final SqlScriptStatus executionStatus = patchAndSqlTableExists && patch.getPatchId()!=null ? autoUpdatePersister.wasSqlCommandAlreadyExecuted(patch.getPatchId(), sqlCommand, newOccurence) : SqlScriptStatus.NOT_EXECUTED;
	        if (executionStatus == SqlScriptStatus.EXECUTED_FINISHED) {
                log.info("Skipping (was already executed before) - occurrence " + newOccurence + ":\n" + sqlCommand);
            } else {
                log.info("Executing:\n" + sqlCommand);
                executeSqlCommand(patch, sqlCommand, autoUpdatePersister, executionStatus);
            }
        }

        if (patchAndSqlTableExists) {
			long stop = System.currentTimeMillis();
			patch.setProcessTime((int)(stop - start));
			patch.setFinishedOn(new Date());
			autoUpdatePersister.markPatchAsFinished(patch);
		}
    }

    /**
     * Executes single SQL command.
     */
    private void executeSqlCommand(final Patch patch, final String sqlStatement, final AutoUpdatePersister autoUpdatePersister, SqlScriptStatus executionStatus) {
        final long startScript = System.currentTimeMillis();
        try {
            final String sqlCommandToExecute = StringProcessor.removeCommentsFromContent(sqlStatement);

            // do this before command is executed
			// in case command triggers implicit commit we need this information in database
			// if no implicit commit is executed and command fails, this insert should be rolled back together with the failing command
			if (patch.isInDb()) {
				if (executionStatus == SqlScriptStatus.NOT_EXECUTED) {
					// finished date is set intentionally - if this SQL statement passes and contains implicit commit
					// the date must be already present in database
					// if implicit commit will not occur and exception is thrown another update in catch clause will reset finished date to NULL again
					autoUpdatePersister.insertSqlScriptToDB(
							patch,
							new SqlCommand(patch.getPatchId(), sqlStatement, 0, new Date(), null)
					);
				}
			}

			// execute command
            jdbcTemplate.execute(sqlCommandToExecute);

	        if (patch.isInDb()) {
		        autoUpdatePersister.updateSqlScriptInDB(patch, new SqlCommand(patch.getPatchId(),
				        sqlStatement, System.currentTimeMillis() - startScript, new Date(), null));
	        }

        } catch (final DataAccessException ex) {
            if (log.isErrorEnabled()) {
                log.error("Failed to execute script: " + sqlStatement, ex);
            }
            if (patch.isInDb()) {
            	// we have to make this update in separate transaction after this one finishes
	            // otherwise connection ends up in deadlock state
	            TransactionSynchronizationManager.registerSynchronization(
			            new TransactionSynchronizationAdapter() {
				            @Override
				            public void afterCompletion(int status) {
				            	if (status == STATUS_ROLLED_BACK) {
				            		TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
				            		txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
				            		txTemplate.execute(new TransactionCallbackWithoutResult() {
							            @Override
							            protected void doInTransactionWithoutResult(TransactionStatus status) {
								            autoUpdatePersister.updateSqlScriptInDB(
										            patch, new SqlCommand(patch.getPatchId(),
												            sqlStatement, System.currentTimeMillis() - startScript,
												            null, ex)
								            );
							            }
						            });
					            }
				            }
			            }
	            );

            }
            throw ex;
        }
    }

}
