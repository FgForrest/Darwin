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
			"/META-INF/lib_db_autoupdate/spring/locker-persister-config.xml"
		}
)
public abstract class DefaultDatabaseLockPersisterTest extends AbstractDbAutoupdateTest {
	@Autowired private DefaultDatabaseLockPersister dbAutoStorageLockPersisterToTest;

	@Test
	public void testGetCurrentDatabaseTime() {
		Date currentTime = dbAutoStorageLockPersisterToTest.getCurrentDatabaseTime();
		assertNotNull(currentTime);
	}

}
