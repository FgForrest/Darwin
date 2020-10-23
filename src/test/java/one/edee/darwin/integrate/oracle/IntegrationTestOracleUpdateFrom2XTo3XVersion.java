package one.edee.darwin.integrate.oracle;

import one.edee.darwin.integrate.IntegrationTestUpdateFrom2xTo3xVersion;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "ORACLE")
@Profile(value = "ORACLE")
public class IntegrationTestOracleUpdateFrom2XTo3XVersion extends IntegrationTestUpdateFrom2xTo3xVersion {
    public IntegrationTestOracleUpdateFrom2XTo3XVersion() {
        super("oracle");
    }

}




