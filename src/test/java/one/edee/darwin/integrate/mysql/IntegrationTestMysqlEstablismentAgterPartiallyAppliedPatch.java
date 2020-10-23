package one.edee.darwin.integrate.mysql;

import one.edee.darwin.integrate.IntegrationTestRecoveringAfterPartiallyAppliedPatch;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class IntegrationTestMysqlEstablismentAgterPartiallyAppliedPatch extends IntegrationTestRecoveringAfterPartiallyAppliedPatch {

	public IntegrationTestMysqlEstablismentAgterPartiallyAppliedPatch() {
        super("mysql");
    }

}
