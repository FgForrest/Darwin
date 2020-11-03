package one.edee.darwin.utils.h2;

import one.edee.darwin.model.Platform;
import one.edee.darwin.utils.AbstractPlatformTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "H2")
@Profile(value = "H2")
public class PlatformTestH2 extends AbstractPlatformTest {

    @Test
    public void shouldRecognizeCorrectPlatform() {
        assertPlatform(Platform.H2);
    }

}
