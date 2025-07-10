package one.edee.darwin.utils;

import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.Darwin;
import one.edee.darwin.model.Patch;
import one.edee.darwin.storage.DarwinStorage;
import one.edee.darwin.storage.DefaultDatabaseDarwinStorage;
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

	public static void assertPatchHasExceptionStored(DarwinStorage darwinStorage, Patch patch) {
		// Get the storage updater to access the JDBC template
		if (darwinStorage instanceof one.edee.darwin.storage.DefaultDatabaseDarwinStorage) {
			// Query the DARWIN_SQL table to check if there are any failed SQL commands with exceptions
			try {
				final JdbcTemplate jdbcTemplate = new JdbcTemplate(((DefaultDatabaseDarwinStorage) darwinStorage).getDataSource());
				String sql = "SELECT A.exception FROM DARWIN_SQL A inner join DARWIN_PATCH B on A.patchId = B.id WHERE B.patchName = ? AND exception IS NOT NULL";
				java.util.List<String> exceptions = jdbcTemplate.queryForList(sql, String.class, patch.getPatchName());

				assertFalse(exceptions.isEmpty(), "Expected to find at least one SQL command with stored exception for patch: " + patch.getPatchName());

				// Verify that at least one exception is not null and not empty
				boolean hasValidException = exceptions.stream().anyMatch(ex -> ex != null && !ex.trim().isEmpty());
				assertTrue(hasValidException, "Expected to find at least one non-empty exception stored for patch: " + patch.getPatchName());

			} catch (Exception e) {
				throw new RuntimeException("Failed to verify exception storage", e);
			}
		} else {
			throw new IllegalArgumentException("Expected DefaultDatabaseDarwinStorage instance");
		}
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
