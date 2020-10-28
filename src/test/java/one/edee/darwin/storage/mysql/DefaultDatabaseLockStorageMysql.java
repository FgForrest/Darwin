package one.edee.darwin.storage.mysql;

import one.edee.darwin.storage.DefaultDatabaseLockStorageTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class DefaultDatabaseLockStorageMysql extends DefaultDatabaseLockStorageTest {

}
