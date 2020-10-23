package one.edee.darwin.integrate.mysql;

import one.edee.darwin.integrate.IntegrationTestUpdateFrom2xTo3xVersion;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class IntegrationTestMysqlUpdateFrom2XTo3XVersion extends IntegrationTestUpdateFrom2xTo3xVersion {
    public IntegrationTestMysqlUpdateFrom2XTo3XVersion() {
        super("mysql");
    }

}




