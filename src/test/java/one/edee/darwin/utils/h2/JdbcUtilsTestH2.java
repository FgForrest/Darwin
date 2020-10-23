package one.edee.darwin.utils.h2;

import one.edee.darwin.utils.JdbcUtilsTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "H2")
@Profile(value = "H2")
public class JdbcUtilsTestH2 extends JdbcUtilsTest{
    @Value("${jdbc.url.h2}")
    private String url;

    @Test
    public void runGetPlatformFromJdbcUrl() throws Exception {
        setJdbcUrl(url);
        testGetPlatformFromJdbcUrl();

    }
}
