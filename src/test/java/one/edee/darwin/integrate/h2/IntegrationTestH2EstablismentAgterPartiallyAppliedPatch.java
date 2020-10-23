package one.edee.darwin.integrate.h2;

import one.edee.darwin.integrate.IntegrationTestRecoveringAfterPartiallyAppliedPatch;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "H2")
@Profile(value = "H2")
public class IntegrationTestH2EstablismentAgterPartiallyAppliedPatch extends IntegrationTestRecoveringAfterPartiallyAppliedPatch {
    public IntegrationTestH2EstablismentAgterPartiallyAppliedPatch() {
        super("h2");
    }

	@Disabled(value = "Oracle can rollback ddl soo this test is unnecessary")
	@Test
    @Override
	public void IntegrationTest_PatchIsPartiallyApplied_AfterCorrectionPatchIsFinishedCompletely() throws Exception {
		super.IntegrationTest_PatchIsPartiallyApplied_AfterCorrectionPatchIsFinishedCompletely();
	}

}
