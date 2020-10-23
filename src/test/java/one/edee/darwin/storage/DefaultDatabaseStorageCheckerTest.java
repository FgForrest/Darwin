package one.edee.darwin.storage;

import com.fg.commons.version.VersionDescriptor;
import one.edee.darwin.AbstractDbAutoupdateTest;
import one.edee.darwin.AutoUpdater;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.DefaultResourceMatcher;
import one.edee.darwin.resources.DefaultResourceNameAnalyzer;
import one.edee.darwin.resources.ResourceAccessorForTest;
import one.edee.darwin.resources.ResourcePatchMediator;
import one.edee.darwin.utils.AutoupdateTestHelper;
import one.edee.darwin.utils.JdbcUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(
        locations = {
                "/META-INF/lib_db_autoupdate/spring/datasource-config.xml",
                "/META-INF/lib_db_autoupdate/spring/db-autoupdate-config.xml",
                "/META-INF/lib_db_autoupdate/spring/guessVersion-config.xml"
        }
)
public abstract class DefaultDatabaseStorageCheckerTest extends AbstractDbAutoupdateTest {

    @Autowired
    @Qualifier(value = "defaultDatabaseAutoUpdatePersister")
    private DefaultDatabaseAutoUpdatePersister defaultDatabaseAutoUpdatePersister;
    @Autowired
    @Qualifier("dbAutoUpdater")
    private AutoUpdater autoUpdater;
    @Autowired
    @Qualifier(value = "dbAutoStorageCheckerToTest")
    private DefaultDatabaseStorageChecker dbAutoStorageCheckerToTest;

	@Test
    public void testGuessVersion() throws Exception {
        dbAutoStorageCheckerToTest.getResourceAccessor().setResourcePath("classpath:/META-INF/lib_db_autoupdate/sql/");
        ((ResourceAccessorForTest) dbAutoStorageCheckerToTest.getResourceAccessor()).setResourcePathForPatch("/META-INF/lib_db_autoupdate/sql-test/upgrade/");
        assertEquals(new VersionDescriptor("3.1"), dbAutoStorageCheckerToTest.guessVersion("lib_db_autoupdate", defaultDatabaseAutoUpdatePersister));
    }

    @Test
    public void testGuessVersionBySelect() throws Exception {
        InputStream is = null;
        try {
            is = new ClassPathResource("META-INF/lib_db_autoupdate/sql-test/guess/" + JdbcUtils.getPlatformFromJdbcUrl(getJdbcTemplate().getDataSource()) + "/init.sql").getInputStream();
            String sql = IOUtils.toString(is, "utf-8");
            getJdbcTemplate().update(sql);
            DefaultDatabaseStorageChecker storageChecker = new DefaultDatabaseStorageChecker(new ResourcePatchMediator(new DefaultResourceMatcher(), new DefaultResourceNameAnalyzer()));
            storageChecker.setResourceAccessor(new DefaultResourceAccessor(autoUpdater.getCtx(), "utf-8", "/META-INF/lib_db_autoupdate/sql-test/guess/"));
            storageChecker.setResourceLoader(autoUpdater.getCtx());
            storageChecker.setResourceMatcher(new DefaultResourceMatcher());
            storageChecker.setResourceNameAnalyzer(new DefaultResourceNameAnalyzer());
            storageChecker.setJdbcTemplate(getJdbcTemplate());
            assertEquals(new VersionDescriptor("1.2"), storageChecker.guessVersion("lib_db_autoupdate", defaultDatabaseAutoUpdatePersister));
        } finally {
            IOUtils.closeQuietly(is);
        }

    }

    @AfterEach
    public void tearDown() throws Exception {
		AutoupdateTestHelper.deleteAllInfrastructuralPages(autoUpdater);
    }
}