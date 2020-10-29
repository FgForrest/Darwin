package one.edee.darwin.integrate;


import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.Darwin;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.Platform;
import one.edee.darwin.resources.ResourceAccessorForTest;
import one.edee.darwin.storage.DarwinStorage;
import one.edee.darwin.storage.DefaultDatabaseStorageUpdater;
import one.edee.darwin.utils.DarwinTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static one.edee.darwin.utils.DarwinTestHelper.assertPatchNotFinishedInDb;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * this test is about recovery from fail command in patch. first Test is fail, second is establishment on first.
 *
 * @author Radek Salay, FG Forest a.s. 6/29/16.
 */
@DirtiesContext
public abstract class IntegrationTestRecoveringAfterPartiallyAppliedPatch extends AbstractDarwinTest {
    @Autowired
    Darwin darwin;
	Platform platform;

    public IntegrationTestRecoveringAfterPartiallyAppliedPatch(Platform platform) {
        this.platform = platform;
    }

    /**
     * this test has all command right, he skip first command in patch because then was done in the previous test{@link #applyBrokenPatch()} and continue
     *
     * @throws Exception
     */
    @Test
    public void IntegrationTest_PatchIsPartiallyApplied_AfterCorrectionPatchIsFinishedCompletely() throws Exception {
		applyBrokenPatch();

		final Patch brokenPatch = new Patch(0, "patch_1.5.sql", "darwin", LocalDateTime.now(), platform, null);
		final DarwinStorage darwinStorage = darwin.getDarwinStorage();

		//broken patch evidence should be present in DB structure
		assertPatchNotFinishedInDb(darwinStorage, brokenPatch);

		//switch resource path to the folder where the same patch has correct contents
        ((ResourceAccessorForTest) darwin.getResourceAccessor()).setResourcePathForPatch("/META-INF/darwin/sql-test/" + platform + "EstablishmentAfterFailSql/withRightSql/");

		//retry autoupdate
        darwin.afterPropertiesSet();

		//check last patch SQL command was successfully applied
		assertPatchIsCompleted();
		//check that patch is marked as finished
		assertTrue(darwinStorage.isPatchFinishedInDb(brokenPatch));
    }

	@AfterEach
    public void tearDown() {
        DefaultDatabaseStorageUpdater d = (DefaultDatabaseStorageUpdater) darwin.getStorageUpdater();
		final JdbcTemplate jdbcTemplate = d.getJdbcTemplate();
		DarwinTestHelper.deleteAllInfrastructuralPages(jdbcTemplate);
        jdbcTemplate.execute("DROP TABLE IF EXISTS TEST");
    }

    /**
     * this run patch which consistent wrong sql command(Create) and fail. But Create is implicit commit so in DB will be record with patch(BrokenFirst)
     *
     */
    private void applyBrokenPatch() {
		try {
			((ResourceAccessorForTest) darwin.getResourceAccessor()).setResourcePathForPatch("/META-INF/darwin/sql-test/" + platform + "EstablishmentAfterFailSql/withWrongSql/");
			darwin.afterPropertiesSet();
			fail("Exception expected here.");
		} catch (BadSqlGrammarException ignored) {
			//exception is anticipated
		}
    }

	private void assertPatchIsCompleted() throws SQLException {
		DefaultDatabaseStorageUpdater updater = (DefaultDatabaseStorageUpdater) darwin.getStorageUpdater();
		assertTrue(updater.getJdbcTemplate().getDataSource().getConnection().getMetaData().getTables(null, null, "TEST", null).next());
	}
}
