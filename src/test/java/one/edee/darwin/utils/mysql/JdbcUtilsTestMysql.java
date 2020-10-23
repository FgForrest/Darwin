package one.edee.darwin.utils.mysql;

import one.edee.darwin.utils.JdbcUtilsTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class JdbcUtilsTestMysql extends JdbcUtilsTest{
    @Value("${jdbc.url.mysql}")
    private String url;

    @Test
    public void runGetPlatformFromJdbcUrl() throws Exception {
        setJdbcUrl(url);
        testGetPlatformFromJdbcUrl();

    }
}
