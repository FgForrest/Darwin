package one.edee.darwin.integrate;

import com.fg.support.test.HostConfigurableContextLoader;
import one.edee.darwin.AutoUpdater;
import one.edee.darwin.model.Patch;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.resources.ResourceAccessorForTest;
import one.edee.darwin.storage.AutoUpdatePersister;
import one.edee.darwin.utils.AutoupdateTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Radek Salay, FG Forest a.s. 7/11/16.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(
		loader = HostConfigurableContextLoader.class,
		locations = {
				"/META-INF/lib_db_autoupdate/spring/datasource-config.xml",
				"/META-INF/lib_db_autoupdate/spring/db-autoupdate-config-test-lazy.xml"
		}
)
public abstract class IntegrationTestUpdateFrom2xTo3xVersion {

	private final String platform;
	@Autowired
	@Qualifier(value = "jdbcTemplateTest")
	JdbcTemplate jdbcTemplate;
	@Autowired
	@Qualifier(value = "dbAutoUpdateResourceAccessor")
	ResourceAccessor resourceAccessor;
	@Autowired
	@Lazy
	AutoUpdater autoUpdater;

	public IntegrationTestUpdateFrom2xTo3xVersion(String platform) {
		this.platform = platform;
	}

	/**
	 * This will setup AutoUpdater to SQL structure of version 3.0.
	 *
	 * @throws Exception
	 */
	@BeforeEach
	public void setUp() throws Exception {
		//set storage where is script for current platform
		AutoupdateTestHelper.deleteAllInfrastructuralPages(jdbcTemplate);
		((ResourceAccessorForTest)resourceAccessor).setResourcePathForPatch("/META-INF/lib_db_autoupdate/sql-test/upgrade/" + platform);
		List<String> setUpSql = resourceAccessor.getTokenizedSQLScriptContentFromResource("/integrateSetUp.sql");

		jdbcTemplate.execute(setUpSql.get(0));
		jdbcTemplate.execute(setUpSql.get(1));
		jdbcTemplate.execute(setUpSql.get(2));
		jdbcTemplate.execute(setUpSql.get(3));

		//set back standard storage
		((ResourceAccessorForTest)resourceAccessor).setResourcePathForPatch("classpath:/META-INF/lib_db_autoupdate/sql/");
	}

	@DirtiesContext
	@Test
	public void IntegrationTest_AutoupdaterStructureInVersion2x_AutoupdaterGetsUpdatedTo3x() throws Exception {
		//patch and SQL tables should not exists
		assertFalse(patchAndSqlTableExists());
		//this will do the upgrade
		autoUpdater.afterPropertiesSet();
		//patch and SQL tables should exists now
		assertTrue(patchAndSqlTableExists());
	}

	@DirtiesContext
	@Test
	public void IntegrationTest_ComponentSetupInVersion2x_ComponentGetsUpgraded() throws Exception {
		final String platform = autoUpdater.getStorageChecker().getPlatform();
		final AutoUpdatePersister autoUpdatePersister = autoUpdater.getAutoUpdatePersister();
		final Patch patch_1_0 = new Patch(0, "patch_1.0.sql", "testovaci", new Date(), platform, null);
		final Patch patch_4_7 = new Patch(0, "patch_4.7.sql", "testovaci", new Date(), platform, null);
		final Patch patch_4_9 = new Patch(0, "patch_4.9.sql", "testovaci", new Date(), platform, null);

		//this will do the upgrade
		autoUpdater.afterPropertiesSet();

		//upgrade component from 2.0 to 4.7
		((ResourceAccessorForTest)resourceAccessor).setResourcePathForPatch("/META-INF/lib_db_autoupdate/sql-test/upgrade/");// change file store, where is patch_3.7 and patch_3.9
		autoUpdater.autoUpdateComponent("testovaci", "4.7");

		//version 1.0 should be setup without execution (to ensure this patch contains error)
		//this is due automatic patch insertion when component is stated to be patched until 2.0
		assertTrue(autoUpdatePersister.isPatchFinishedInDb(patch_1_0));
		//version 4.7 should be executed
		assertTrue(autoUpdatePersister.isPatchFinishedInDb(patch_4_7));
		//version 4.9 should not be executed - we wanted to upgrade component only to version 4.7
		assertFalse(autoUpdatePersister.isPatchFinishedInDb(patch_4_9));
		//do update it to 4.9 version
		autoUpdater.autoUpdateComponent("testovaci", "4.9");
		//now patch should be present
		assertTrue(autoUpdatePersister.isPatchFinishedInDb(patch_4_9));

	}

	@AfterEach
	public void tearDown() throws Exception {
		AutoupdateTestHelper.deleteAllInfrastructuralPages(jdbcTemplate);
	}

	private boolean patchAndSqlTableExists() {
		try {
			jdbcTemplate.queryForList("SELECT * FROM T_DB_AUTOUPDATE_PATCH");
			jdbcTemplate.queryForList("SELECT * FROM T_DB_AUTOUPDATE_SQL");
			return true;
		} catch(BadSqlGrammarException ex) {
			return false;
		}
	}
}
