package one.edee.darwin.utils;

import one.edee.darwin.Darwin;
import one.edee.darwin.model.Patch;
import one.edee.darwin.storage.AbstractDatabaseStorage;
import one.edee.darwin.storage.DarwinStorage;
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

	public static void assertPatchNotPresentInDb(DarwinStorage darwinStorage, Patch patch) {
		assertFalse(darwinStorage.isPatchFinishedInDb(patch));
	}

	public static void assertPatchFinishedInDb(DarwinStorage darwinStorage, Patch patch) {
		assertTrue(darwinStorage.isPatchFinishedInDb(patch));
	}

	public static void assertPatchNotFinishedInDb(DarwinStorage darwinStorage, Patch patch) {
		assertFalse(darwinStorage.isPatchFinishedInDb(patch));
	}

	public static void deleteAllInfrastructuralPages(Darwin darwin) {
		DefaultDatabaseStorageUpdater updater = (DefaultDatabaseStorageUpdater) darwin.getStorageUpdater();
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
