package one.edee.darwin.storage;

import one.edee.darwin.AbstractDbAutoupdateTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Description
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
@ContextConfiguration(
		locations = {
				"/META-INF/darwin/spring/locker-persister-config.xml"
		}
)
public abstract class DefaultDatabaseLockStorageTest extends AbstractDbAutoupdateTest {
	@Autowired private DefaultDatabaseLockStorage dbAutoStorageLockPersisterToTest;

	@Test
	public void testGetCurrentDatabaseTime() {
		Date currentTime = dbAutoStorageLockPersisterToTest.getCurrentDatabaseTime();
		assertNotNull(currentTime);
	}

}
