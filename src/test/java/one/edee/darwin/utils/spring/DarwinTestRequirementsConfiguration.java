package one.edee.darwin.utils.spring;

import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.DefaultResourceMatcher;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.resources.ResourcePatchMediator;
import one.edee.darwin.storage.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Contains beans required for tests.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
@Configuration
public class DarwinTestRequirementsConfiguration {

    @Bean
    public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        final PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setLocation(new ClassPathResource("test.properties"));
        return configurer;
    }

    @Bean
    public ScheduledExecutorService scheduledExecutor() {
        return new ScheduledThreadPoolExecutor(3);
    }

    @Bean
    public DefaultResourceAccessor darwinResourceAccessor(ResourceLoader resourceLoader) {
        return new DefaultResourceAccessor(resourceLoader, "UTF-8", "classpath:/META-INF/darwin/sql/");
    }

    @Bean
    public DefaultDatabaseStorageChecker storageChecker(DataSource dataSource, PlatformTransactionManager transactionManager, @Qualifier("darwinResourceAccessor") ResourceAccessor resourceAccessor) {
        final DefaultDatabaseStorageChecker storageChecker = new DefaultDatabaseStorageChecker(
            new ResourcePatchMediator(
                new DefaultResourceMatcher()
            )
        );
        storageChecker.setDataSource(dataSource);
        storageChecker.setTransactionManager(transactionManager);
        storageChecker.setResourceAccessor(resourceAccessor);
        return storageChecker;
    }

    @Bean
    public DefaultDatabaseStorageUpdater databaseStorageUpdater(DataSource dataSource, PlatformTransactionManager transactionManager, @Qualifier("darwinResourceAccessor") ResourceAccessor resourceAccessor, StorageChecker storageChecker) {
        final DefaultDatabaseStorageUpdater storageUpdater = new DefaultDatabaseStorageUpdater(storageChecker);
        storageUpdater.setDataSource(dataSource);
        storageUpdater.setTransactionManager(transactionManager);
        storageUpdater.setResourceAccessor(resourceAccessor);
        return storageUpdater;
    }

    @Bean
    public DarwinStorage darwinStorage(DataSource dataSource, PlatformTransactionManager transactionManager, StorageChecker storageChecker, ResourceLoader resourceLoader, @Qualifier("darwinResourceAccessor") ResourceAccessor resourceAccessor) {
        final DefaultDatabaseDarwinStorage storage = new DefaultDatabaseDarwinStorage(
            new DefaultResourceMatcher(),
            storageChecker
        );
        storage.setDataSource(dataSource);
        storage.setTransactionManager(transactionManager);
        storage.setResourceLoader(resourceLoader);
        storage.setResourceAccessor(resourceAccessor);
        return storage;
    }

}
