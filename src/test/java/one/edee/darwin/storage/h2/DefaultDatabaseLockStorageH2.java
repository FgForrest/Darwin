package one.edee.darwin.storage.h2;

import one.edee.darwin.storage.DefaultDatabaseLockStorageTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "H2")
@Profile(value = "H2")
public class DefaultDatabaseLockStorageH2 extends DefaultDatabaseLockStorageTest {

}
