package one.edee.darwin.integrate.h2;

import one.edee.darwin.integrate.IntegrationTestOlderPatchApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;


/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(profiles = "H2")
@Profile(value = "H2")
public class IntegrationTestH2OldPatchApplication extends IntegrationTestOlderPatchApplication {

}
