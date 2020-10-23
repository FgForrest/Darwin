package one.edee.darwin.storage.oracle;

import one.edee.darwin.storage.DefaultDatabaseStorageCheckerTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "ORACLE")
@Profile(value = "ORACLE")
public class DefaultDatabaseStorageCheckerOracle extends DefaultDatabaseStorageCheckerTest{


}
