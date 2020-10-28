package one.edee.darwin.integrate;

import one.edee.darwin.AbstractDbAutoupdateTest;
import one.edee.darwin.Darwin;
import one.edee.darwin.model.Patch;
import one.edee.darwin.resources.ResourceAccessorForTest;
import one.edee.darwin.storage.DarwinStorage;
import one.edee.darwin.storage.StorageChecker;
import one.edee.darwin.utils.AutoupdateTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Date;

import static one.edee.darwin.utils.AutoupdateTestHelper.assertPatchFinishedInDb;
import static one.edee.darwin.utils.AutoupdateTestHelper.assertPatchNotPresentInDb;


/**
 * @author Radek Salay, FG Forest a.s. 6/28/16.
 */
@DirtiesContext
public abstract class IntegrationTestOlderPatchApplication extends AbstractDbAutoupdateTest {
    @Autowired
    Darwin darwin;

    @Autowired @Qualifier(value = "jdbcTemplateTest")
    JdbcTemplate jdbcTemplate;

	@Test
    public void IntegrationTest_LowerPatchSuddenlyAppears_andIsRetrospectivelyApplied() throws Exception {
        final DarwinStorage darwinStorage = darwin.getDarwinStorage();
        final StorageChecker storageChecker = darwin.getStorageChecker();
        final String platform = storageChecker.getPlatform();

		//this patch is not originally in place but appear there suddenly
		final Patch oldPatch = new Patch("patch_1.2.sql", "lib_db_autoupdate", platform, new Date());

		//check that retrospective patch isn't there
		assertPatchNotPresentInDb(darwinStorage, oldPatch);

		//switch checker to another folder where older patch is present
        ((ResourceAccessorForTest) darwin.getResourceAccessor()).setResourcePathForPatch("/META-INF/darwin/sql-test/" + platform + "ApplicationOlderPach/");

		//retry autoupdate
        darwin.afterPropertiesSet();

		//check patch was retrospectively applied
		assertPatchFinishedInDb(darwinStorage, oldPatch);
	}

	@AfterEach
    public void tearDown() throws Exception {
		AutoupdateTestHelper.deleteAllInfrastructuralPages(darwin);
    }

}
