package one.edee.darwin.utils;

import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.Darwin;
import one.edee.darwin.model.Patch;
import one.edee.darwin.storage.DarwinStorage;
import one.edee.darwin.storage.DefaultDatabaseStorageUpdater;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Objects;

import static one.edee.darwin.model.Platform.ORACLE;
import static one.edee.darwin.model.Platform.getPlatformFromJdbcUrl;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No documentation needed, just look at the methods.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2016
 */
@CommonsLog
public class DarwinTestHelper {

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
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(updater.getDataSource());
		deleteAllInfrastructuralPages(jdbcTemplate);
	}

	public static void deleteAllInfrastructuralPages(JdbcTemplate jdbcTemplate) {
		safelyExecute(jdbcTemplate, "DROP TABLE DARWIN_SQL");
		safelyExecute(jdbcTemplate, "DROP TABLE DARWIN_PATCH");
		safelyExecute(jdbcTemplate, "DROP TABLE DARWIN_LOCK");
		safelyExecute(jdbcTemplate, "DROP TABLE DARWIN");
		if (ORACLE.equals(getPlatformFromJdbcUrl(Objects.requireNonNull(jdbcTemplate.getDataSource())))) {
			safelyExecute(jdbcTemplate, "DROP SEQUENCE SQ_DARWIN_PATCH");
			safelyExecute(jdbcTemplate, "DROP SEQUENCE SQ_DARWIN_SQL");
		}
	}

	private static void safelyExecute(JdbcTemplate jdbcTemplate, String sql) {
		try {
			jdbcTemplate.execute(sql);
		} catch (BadSqlGrammarException ex) {
			//might happen, just log
			log.warn(ex.getMessage(), ex);
		}
	}
}
