package one.edee.darwin.storage;

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.Darwin;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.version.VersionDescriptor;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.DefaultResourceMatcher;
import one.edee.darwin.resources.DefaultResourceNameAnalyzer;
import one.edee.darwin.resources.ResourceAccessorForTest;
import one.edee.darwin.resources.ResourcePatchMediator;
import one.edee.darwin.utils.DarwinTestHelper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static one.edee.darwin.model.Platform.getPlatformFromJdbcUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(
        locations = {
		        "/META-INF/darwin/spring/datasource-config.xml",
		        "/META-INF/darwin/spring/db-autoupdate-config.xml",
		        "/META-INF/darwin/spring/guessVersion-config.xml"
        }
)
public abstract class DefaultDatabaseStorageCheckerTest extends AbstractDarwinTest {

    @Autowired
    @Qualifier(value = "defaultDatabaseAutoUpdatePersister")
    private DefaultDatabaseDarwinStorage defaultDatabaseAutoUpdatePersister;
    @Autowired
    @Qualifier("darwin")
    private Darwin darwin;
    @Autowired
    @Qualifier(value = "dbAutoStorageCheckerToTest")
    private DefaultDatabaseStorageChecker dbAutoStorageCheckerToTest;

	@Test
    public void testGuessVersion() {
	    /* TODO JNO, možná odebrat? */
        // dbAutoStorageCheckerToTest.getResourceAccessor().setResourcePath("classpath:/META-INF/darwin/sql/");
        ((ResourceAccessorForTest) dbAutoStorageCheckerToTest.getResourceAccessor()).setResourcePathForPatch("/META-INF/darwin/sql-test/upgrade/");
        assertEquals(new VersionDescriptor("3.1"), dbAutoStorageCheckerToTest.guessVersion("darwin", defaultDatabaseAutoUpdatePersister));
    }

    @Test
    public void testGuessVersionBySelect() throws Exception {
        final Platform platform = getPlatformFromJdbcUrl(getJdbcTemplate().getDataSource());
        try (final InputStream is = new ClassPathResource("META-INF/darwin/sql-test/guess/" + platform.getFolderName() + "/init.sql").getInputStream()) {
            String sql = IOUtils.toString(is, StandardCharsets.UTF_8);
            getJdbcTemplate().update(sql);
            DefaultDatabaseStorageChecker storageChecker = new DefaultDatabaseStorageChecker(new ResourcePatchMediator(new DefaultResourceMatcher(), new DefaultResourceNameAnalyzer()));
            storageChecker.setResourceAccessor(new DefaultResourceAccessor(darwin.getApplicationContext(), "utf-8", "/META-INF/darwin/sql-test/guess/"));
            storageChecker.setResourceLoader(darwin.getApplicationContext());
            storageChecker.setResourceMatcher(new DefaultResourceMatcher());
            storageChecker.setResourceNameAnalyzer(new DefaultResourceNameAnalyzer());
            storageChecker.setJdbcTemplate(getJdbcTemplate());
            assertEquals(new VersionDescriptor("1.2"), storageChecker.guessVersion("darwin", defaultDatabaseAutoUpdatePersister));
        }
    }

    @AfterEach
    public void tearDown() {
		DarwinTestHelper.deleteAllInfrastructuralPages(darwin);
    }
}