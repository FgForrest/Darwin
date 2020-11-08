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
import one.edee.darwin.storage.StorageChecker;
import one.edee.darwin.utils.DarwinTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static one.edee.darwin.utils.DarwinTestHelper.assertPatchFinishedInDb;
import static one.edee.darwin.utils.DarwinTestHelper.assertPatchNotPresentInDb;


/**
 * @author Radek Salay, FG Forest a.s. 6/28/16.
 */
@ContextConfiguration(
		classes = {
				DarwinConfiguration.class
		}
)
@DirtiesContext
public abstract class IntegrationTestOlderPatchApplication extends AbstractDarwinTest {
    @Autowired
    Darwin darwin;

	@Test
    public void IntegrationTest_LowerPatchSuddenlyAppears_andIsRetrospectivelyApplied() {
        final DarwinStorage darwinStorage = darwin.getDarwinStorage();
        final StorageChecker storageChecker = darwin.getStorageChecker();
        final Platform platform = storageChecker.getPlatform();
        darwin.setModelVersion(new SchemaVersion("darwin", "1.2"));

		//this patch is not originally in place but appear there suddenly
		final Patch oldPatch = new Patch("patch_1.2.sql", "darwin", platform, LocalDateTime.now());

		//check that retrospective patch isn't there
		assertPatchNotPresentInDb(darwinStorage, oldPatch);

		final ResourceAccessorForTest resourceAccessor4Test = new ResourceAccessorForTest(darwin.getApplicationContext(), "UTF-8", "classpath:/META-INF/darwin/sql/");
		resourceAccessor4Test.setResourcePathForPatch("/META-INF/darwin/sql-test/" + platform.getFolderName() + "ApplicationOlderPatch/");
		//switch checker to another folder where older patch is present
		darwin.setResourceAccessor(resourceAccessor4Test);
		((DefaultDatabaseStorageUpdater)darwin.getStorageUpdater()).setResourceAccessor(resourceAccessor4Test);

		//retry autoupdate
        darwin.evolve();

		//check patch was retrospectively applied
		assertPatchFinishedInDb(darwinStorage, oldPatch);
	}

	@AfterEach
    public void tearDown() {
		DarwinTestHelper.deleteAllInfrastructuralPages(darwin);
    }

}
