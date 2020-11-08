package one.edee.darwin.storage;

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.storage.DefaultDatabaseLockStorageTest.TestConfiguration;
import one.edee.darwin.utils.spring.DataSourceConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ContextConfiguration(
	classes = {
		DataSourceConfiguration.class,
		TestConfiguration.class
	}
)
public abstract class DefaultDatabaseLockStorageTest extends AbstractDarwinTest {
	@Autowired private DefaultDatabaseLockStorage dbAutoStorageLockPersisterToTest;

	@Configuration
	public static class TestConfiguration {

		@Bean
		public DefaultDatabaseLockStorage databaseLockStorage(DataSource dataSource, PlatformTransactionManager transactionManager, ResourceAccessor resourceAccessor) {
			final DefaultDatabaseLockStorage lockStorage = new DefaultDatabaseLockStorage();
			lockStorage.setDataSource(dataSource);
			lockStorage.setTransactionManager(transactionManager);
			lockStorage.setResourceAccessor(resourceAccessor);
			return lockStorage;
		}

		@Bean
		public ResourceAccessor resourceAccessor(ResourceLoader resourceLoader) {
			return new DefaultResourceAccessor(resourceLoader, "UTF-8", "classpath:/META-INF/darwin/sql");
		}

	}

	@Test
	public void testGetCurrentDatabaseTime() {
		LocalDateTime currentTime = dbAutoStorageLockPersisterToTest.getCurrentDatabaseTime();
		assertNotNull(currentTime);
	}

}
