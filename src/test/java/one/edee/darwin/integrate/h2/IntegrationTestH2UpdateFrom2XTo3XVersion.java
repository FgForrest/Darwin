package one.edee.darwin.integrate.h2;

import one.edee.darwin.integrate.IntegrationTestUpdateFrom2xTo3xVersion;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "H2")
@Profile(value = "H2")
public class IntegrationTestH2UpdateFrom2XTo3XVersion extends IntegrationTestUpdateFrom2xTo3xVersion {
    public IntegrationTestH2UpdateFrom2XTo3XVersion() {
        super("h2");
    }

}




