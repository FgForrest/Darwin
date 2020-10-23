package one.edee.darwin.locker.oracle;

import one.edee.darwin.locker.LockerTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "ORACLE")
@Profile(value = "ORACLE")
public class LockerTestOracle extends LockerTest {

}
