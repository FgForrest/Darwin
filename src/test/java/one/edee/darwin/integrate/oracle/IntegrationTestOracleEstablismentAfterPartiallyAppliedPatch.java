package one.edee.darwin.integrate.oracle;

import one.edee.darwin.integrate.IntegrationTestRecoveringAfterPartiallyAppliedPatch;
import one.edee.darwin.model.Platform;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "ORACLE")
@Profile(value = "ORACLE")
public class IntegrationTestOracleEstablismentAfterPartiallyAppliedPatch extends IntegrationTestRecoveringAfterPartiallyAppliedPatch {

    public IntegrationTestOracleEstablismentAfterPartiallyAppliedPatch() {
        super(Platform.ORACLE);
    }

	@Disabled(value = "Oracle can rollback ddl soo this test is unnecessary")
	@Test
    @Override
	public void IntegrationTest_PatchIsPartiallyApplied_AfterCorrectionPatchIsFinishedCompletely() throws Exception {
		super.IntegrationTest_PatchIsPartiallyApplied_AfterCorrectionPatchIsFinishedCompletely();
	}

}
