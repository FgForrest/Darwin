package one.edee.darwin.integrate.h2;

import one.edee.darwin.integrate.IntegrationTestRecoveringAfterPartiallyAppliedPatch;
import one.edee.darwin.model.Platform;
import one.edee.darwin.spring.DarwinConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ContextConfiguration(
		classes = {
				DarwinConfiguration.class
		}
)
@ActiveProfiles(value = "H2")
@Profile(value = "H2")
public class IntegrationTestH2EstablismentAgterPartiallyAppliedPatch extends IntegrationTestRecoveringAfterPartiallyAppliedPatch {

    public IntegrationTestH2EstablismentAgterPartiallyAppliedPatch() {
        super(Platform.H2);
    }

	@Disabled(value = "Oracle can rollback ddl soo this test is unnecessary")
	@Test
    @Override
	public void IntegrationTest_PatchIsPartiallyApplied_AfterCorrectionPatchIsFinishedCompletely() throws Exception {
		super.IntegrationTest_PatchIsPartiallyApplied_AfterCorrectionPatchIsFinishedCompletely();
	}

}
