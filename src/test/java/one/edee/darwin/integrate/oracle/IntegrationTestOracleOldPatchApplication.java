package one.edee.darwin.integrate.oracle;

import one.edee.darwin.integrate.IntegrationTestOlderPatchApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;


/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(profiles = "ORACLE")
@Profile(value = "ORACLE")
public class IntegrationTestOracleOldPatchApplication extends IntegrationTestOlderPatchApplication {

}
