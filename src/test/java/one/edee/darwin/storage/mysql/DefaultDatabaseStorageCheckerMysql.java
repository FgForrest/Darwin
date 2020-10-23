package one.edee.darwin.storage.mysql;

import one.edee.darwin.storage.DefaultDatabaseStorageCheckerTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class DefaultDatabaseStorageCheckerMysql extends DefaultDatabaseStorageCheckerTest{


}
