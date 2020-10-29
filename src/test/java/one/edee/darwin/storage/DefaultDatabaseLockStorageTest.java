package one.edee.darwin.storage;

import one.edee.darwin.AbstractDarwinTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ContextConfiguration(
		locations = {
				"/META-INF/darwin/spring/locker-persister-config.xml"
		}
)
public abstract class DefaultDatabaseLockStorageTest extends AbstractDarwinTest {
	@Autowired private DefaultDatabaseLockStorage dbAutoStorageLockPersisterToTest;

	@Test
	public void testGetCurrentDatabaseTime() {
		LocalDateTime currentTime = dbAutoStorageLockPersisterToTest.getCurrentDatabaseTime();
		assertNotNull(currentTime);
	}

}
