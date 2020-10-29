package one.edee.darwin.utils.mysql;

import one.edee.darwin.model.Platform;
import one.edee.darwin.utils.AbstractPlatformTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class AbstractPlatformTestMysql extends AbstractPlatformTest {

    @Test
    public void shouldRecognizeCorrectPlatform() {
        assertPlatform(Platform.MYSQL);
    }

}
