package one.edee.darwin.integrate.mysql;

import one.edee.darwin.integrate.IntegrationTestRecoveringAfterPartiallyAppliedPatch;
import one.edee.darwin.model.Platform;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class IntegrationTestMysqlEstablismentAfterPartiallyAppliedPatch extends IntegrationTestRecoveringAfterPartiallyAppliedPatch {

	public IntegrationTestMysqlEstablismentAfterPartiallyAppliedPatch() {
        super(Platform.MYSQL);
    }

}
