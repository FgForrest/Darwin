package one.edee.darwin.storage;

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.Darwin;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.version.VersionDescriptor;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.DefaultResourceMatcher;
import one.edee.darwin.resources.DefaultResourceNameAnalyzer;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.resources.ResourceAccessorForTest;
import one.edee.darwin.resources.ResourceMatcher;
import one.edee.darwin.resources.ResourceNameAnalyzer;
import one.edee.darwin.resources.ResourcePatchMediator;
import one.edee.darwin.storage.DefaultDatabaseStorageCheckerTest.TestConfiguration;
import one.edee.darwin.utils.DarwinTestHelper;
import one.edee.darwin.utils.spring.DarwinTestConfiguration;
import one.edee.darwin.utils.spring.DataSourceConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static one.edee.darwin.model.Platform.getPlatformFromJdbcUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration(
        classes = {
                DataSourceConfiguration.class,
                DarwinTestConfiguration.class,
                TestConfiguration.class
        }
)
public abstract class DefaultDatabaseStorageCheckerTest extends AbstractDarwinTest implements IOUtils {
    @Autowired private DarwinStorage darwinStorage;
    @Autowired private Darwin darwin;
    @Autowired private DefaultDatabaseStorageChecker tested;
    @Autowired private DataSource dataSource;
    @Autowired private PlatformTransactionManager transactionManager;

	@Test
    @DirtiesContext
    public void testGuessVersion() {
        ((ResourceAccessorForTest) tested.getResourceAccessor()).setResourcePathForPatch("/META-INF/darwin/sql-test/upgrade/");
        assertEquals(new VersionDescriptor("1.1"), tested.guessVersion("darwin", darwinStorage));
    }

    @Test
    @DirtiesContext
    public void testGuessVersionBySelect() throws Exception {
        final Platform platform = getPlatformFromJdbcUrl(getJdbcTemplate().getDataSource());
        try (final InputStream is = new ClassPathResource("META-INF/darwin/sql-test/guess/" + platform.getFolderName() + "/init.sql").getInputStream()) {
            String sql = toString(is, StandardCharsets.UTF_8);
            getJdbcTemplate().update(sql);
            DefaultDatabaseStorageChecker storageChecker = new DefaultDatabaseStorageChecker(
                    new ResourcePatchMediator(
                            new DefaultResourceMatcher(),
                            new DefaultResourceNameAnalyzer()
                    )
            );
            storageChecker.setResourceAccessor(new DefaultResourceAccessor(darwin.getApplicationContext(), "utf-8", "/META-INF/darwin/sql-test/guess/"));
            storageChecker.setResourceLoader(darwin.getApplicationContext());
            storageChecker.setResourceMatcher(new DefaultResourceMatcher());
            storageChecker.setResourceNameAnalyzer(new DefaultResourceNameAnalyzer());
            storageChecker.setDataSource(dataSource);
            storageChecker.setTransactionManager(transactionManager);
            assertEquals(new VersionDescriptor("1.2"), storageChecker.guessVersion("darwin", darwinStorage));
        }
    }

    @AfterEach
    public void tearDown() {
		DarwinTestHelper.deleteAllInfrastructuralPages(darwin);
    }

    @Configuration
    public static class TestConfiguration {

	    @Bean
        public ResourceMatcher resourceMatcher() {
	        return new DefaultResourceMatcher();
        }

        @Bean
        public ResourceNameAnalyzer resourceNameAnalyzer() {
	        return new DefaultResourceNameAnalyzer();
        }

        @Bean
        public ResourceAccessor resourceAccessor(ResourceLoader resourceLoader) {
	        return new ResourceAccessorForTest(resourceLoader, "UTF-8", "classpath:/META-INF/darwin/sql/");
        }

        @Bean
        public DarwinStorage darwinStorage(StorageChecker storageChecker, ResourceAccessor resourceAccessor, DataSource dataSource, PlatformTransactionManager transactionManager) {
            final DefaultDatabaseDarwinStorage storage = new DefaultDatabaseDarwinStorage(
                    new DefaultResourceNameAnalyzer(),
                    storageChecker
            );
            storage.setDataSource(dataSource);
            storage.setTransactionManager(transactionManager);
            storage.setResourceAccessor(resourceAccessor);
            return storage;
        }

        @Bean
        public DefaultDatabaseStorageChecker storageChecker(DataSource dataSource, PlatformTransactionManager transactionManager, ResourceAccessor resourceAccessor, ResourceLoader resourceLoader) {
            final DefaultDatabaseStorageChecker storageChecker = new DefaultDatabaseStorageChecker(
                    new ResourcePatchMediator(
                            resourceMatcher(),
                            resourceNameAnalyzer()
                    )
            );
            storageChecker.setDataSource(dataSource);
            storageChecker.setTransactionManager(transactionManager);
            storageChecker.setResourceAccessor(resourceAccessor);
            storageChecker.setResourceLoader(resourceLoader);
            storageChecker.setResourceMatcher(resourceMatcher());
            storageChecker.setResourceNameAnalyzer(resourceNameAnalyzer());
            return storageChecker;
        }

    }

}