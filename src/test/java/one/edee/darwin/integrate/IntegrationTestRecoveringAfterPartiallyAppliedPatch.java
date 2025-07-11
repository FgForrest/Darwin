package one.edee.darwin.integrate;


import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.Darwin;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.SchemaVersion;
import one.edee.darwin.resources.ResourceAccessorForTest;
import one.edee.darwin.spring.DarwinConfiguration;
import one.edee.darwin.storage.DarwinStorage;
import one.edee.darwin.storage.DefaultDatabaseStorageUpdater;
import one.edee.darwin.utils.DarwinTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static one.edee.darwin.utils.DarwinTestHelper.assertPatchHasExceptionStored;
import static one.edee.darwin.utils.DarwinTestHelper.assertPatchNotFinishedInDb;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * this test is about recovery from fail command in patch. first Test is fail, second is establishment on first.
 *
 * @author Radek Salay, FG Forest a.s. 6/29/16.
 */
@ContextConfiguration(
		classes = {
				DarwinConfiguration.class
		}
)
@DirtiesContext
public abstract class IntegrationTestRecoveringAfterPartiallyAppliedPatch extends AbstractDarwinTest {
    @Autowired
    Darwin darwin;
	Platform platform;

    public IntegrationTestRecoveringAfterPartiallyAppliedPatch(Platform platform) {
        this.platform = platform;
    }

    /**
     * this test has all command right, he skip first command in patch because then was done in the previous test{@link #applyBrokenPatch(ResourceAccessorForTest)} and continue
     *
     * @throws Exception
     */
    @Test
    public void IntegrationTest_PatchIsPartiallyApplied_AfterCorrectionPatchIsFinishedCompletely() throws Exception {
    	darwin.setModelVersion(new SchemaVersion("darwin", "1.5"));
	    final ResourceAccessorForTest resourceAccessor4Test = new ResourceAccessorForTest(darwin.getApplicationContext(), "UTF-8", "classpath:/META-INF/darwin/sql/");
	    //switch checker to test one
	    darwin.setResourceAccessor(resourceAccessor4Test);
	    ((DefaultDatabaseStorageUpdater)darwin.getStorageUpdater()).setResourceAccessor(resourceAccessor4Test);

		applyBrokenPatch(resourceAccessor4Test);

		final Patch brokenPatch = new Patch(0, "patch_1.5.sql", "darwin", LocalDateTime.now(), platform, null);
		final DarwinStorage darwinStorage = darwin.getDarwinStorage();

		//broken patch evidence should be present in DB structure
		assertPatchNotFinishedInDb(darwinStorage, brokenPatch);

		//verify that the exception from broken patch is correctly written to the database
		assertPatchHasExceptionStored(darwinStorage, brokenPatch);

		//switch resource path to the folder where the same patch has correct contents
	    resourceAccessor4Test.setResourcePathForPatch("/META-INF/darwin/sql-test/" + platform.getFolderName() + "EstablishmentAfterFailSql/withRightSql/");

		//retry autoupdate
        darwin.evolve();

		//check last patch SQL command was successfully applied
		assertPatchIsCompleted();
		//check that patch is marked as finished
		assertTrue(darwinStorage.isPatchFinishedInDb(brokenPatch));
    }

	@AfterEach
    public void tearDown() {
        DefaultDatabaseStorageUpdater d = (DefaultDatabaseStorageUpdater) darwin.getStorageUpdater();
		final JdbcTemplate jdbcTemplate = new JdbcTemplate(d.getDataSource());
		DarwinTestHelper.deleteAllInfrastructuralPages(jdbcTemplate);
        jdbcTemplate.execute("DROP TABLE IF EXISTS TEST");
    }

    /**
     * this run patch which consistent wrong sql command(Create) and fail. But Create is implicit commit so in DB will be record with patch(BrokenFirst)
     *
     */
    private void applyBrokenPatch(ResourceAccessorForTest resourceAccessor4Test) {
		try {
			resourceAccessor4Test.setResourcePathForPatch("/META-INF/darwin/sql-test/" + platform.getFolderName() + "EstablishmentAfterFailSql/withWrongSql/");
			darwin.evolve();
			fail("Exception expected here.");
		} catch (UncategorizedSQLException |  BadSqlGrammarException ignored) {
			//exception is anticipated
		}
    }

	private void assertPatchIsCompleted() throws SQLException {
		DefaultDatabaseStorageUpdater updater = (DefaultDatabaseStorageUpdater) darwin.getStorageUpdater();
		assertTrue(updater.getDataSource().getConnection().getMetaData().getTables(null, null, "TEST", null).next());
	}
}
