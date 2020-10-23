package one.edee.darwin.utils;

import one.edee.darwin.AutoUpdater;
import one.edee.darwin.model.Patch;
import one.edee.darwin.storage.AbstractDatabaseStorage;
import one.edee.darwin.storage.AutoUpdatePersister;
import one.edee.darwin.storage.DefaultDatabaseStorageUpdater;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No documentation needed, just look at the methods.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2016
 */
public class AutoupdateTestHelper {

	public static void assertPatchNotPresentInDb(AutoUpdatePersister autoUpdatePersister, Patch patch) {
		assertFalse(autoUpdatePersister.isPatchFinishedInDb(patch));
	}

	public static void assertPatchFinishedInDb(AutoUpdatePersister autoUpdatePersister, Patch patch) {
		assertTrue(autoUpdatePersister.isPatchFinishedInDb(patch));
	}

	public static void assertPatchNotFinishedInDb(AutoUpdatePersister autoUpdatePersister, Patch patch) {
		assertFalse(autoUpdatePersister.isPatchFinishedInDb(patch));
	}

	public static void deleteAllInfrastructuralPages(AutoUpdater autoUpdater) {
		DefaultDatabaseStorageUpdater updater = (DefaultDatabaseStorageUpdater) autoUpdater.getStorageUpdater();
		final JdbcTemplate jdbcTemplate = updater.getJdbcTemplate();
		deleteAllInfrastructuralPages(jdbcTemplate);
	}

	public static void deleteAllInfrastructuralPages(JdbcTemplate jdbcTemplate) {
		safelyExecute(jdbcTemplate, "DROP TABLE T_DB_AUTOUPDATE_SQL");
		safelyExecute(jdbcTemplate, "DROP TABLE T_DB_AUTOUPDATE_PATCH");
		safelyExecute(jdbcTemplate, "DROP TABLE T_DB_AUTOUPDATE_LOCK");
		safelyExecute(jdbcTemplate, "DROP TABLE T_DB_AUTOUPDATE");
		if (AbstractDatabaseStorage.ORACLE.equals(JdbcUtils.getPlatformFromJdbcUrl(jdbcTemplate.getDataSource()))) {
			safelyExecute(jdbcTemplate, "DROP SEQUENCE SQ_T_DB_AUTOUPDATE_PATCH");
			safelyExecute(jdbcTemplate, "DROP SEQUENCE SQ_T_DB_AUTOUPDATE_SQL");
		}
	}

	private static void safelyExecute(JdbcTemplate jdbcTemplate, String sql) {
		try {
			jdbcTemplate.execute(sql);
		} catch (BadSqlGrammarException ignored) {
			//might happen
		}
	}
}
