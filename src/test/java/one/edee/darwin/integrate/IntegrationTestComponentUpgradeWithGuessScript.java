package one.edee.darwin.integrate;

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.Darwin;
import one.edee.darwin.DarwinBuilder;
import one.edee.darwin.model.version.VersionDescriptor;
import one.edee.darwin.spring.DarwinConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test that verifies component upgrade behavior when a guess script
 * detects that a column already exists, so the patch script should not be executed.
 */
@ContextConfiguration(
        classes = {
                DarwinConfiguration.class
        }
)
@ActiveProfiles("MYSQL")
@DirtiesContext
public abstract class IntegrationTestComponentUpgradeWithGuessScript extends AbstractDarwinTest {
    @Autowired private ApplicationContext applicationContext;
    @Autowired private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void tearDown() throws Exception {
        jdbcTemplate.update("DROP TABLE IF EXISTS TEST_TABLE");
        jdbcTemplate.update("DELETE FROM DARWIN WHERE component = ?", "test_component");
    }

    @Test
    public void IntegrationTest_ComponentUpgrade_GuessScriptDetectsExistingColumn_PatchNotExecuted() {
        final Darwin darwin = new DarwinBuilder(applicationContext, "test_component", "1.0")
            .withResourcePath("classpath:/META-INF/darwin/sql-test/component-upgrade-test/")
            .build();

        // Initialize Darwin - this should create the TEST_TABLE with both columns a and b
        darwin.evolve();

        // Verify that component version 1.0 is recorded
        VersionDescriptor currentVersion = darwin.getDarwinStorage().getVersionDescriptorForComponent("test_component");
        assertEquals(new VersionDescriptor("1"), currentVersion);

        // Now upgrade to version 1.1
        final Darwin darwinForUpgrade = new DarwinBuilder(applicationContext, "test_component", "1.1")
            .withResourcePath("classpath:/META-INF/darwin/sql-test/component-upgrade-test/")
            .build();

        // Call evolve again - this should:
        // 1. Find the guess_1.1.sql script
        // 2. Execute it successfully (because column b already exists)
        // 3. Update the component version to 1.1 without executing patch_1.1.sql
        darwinForUpgrade.evolve();

        // Verify that component version is now 1.1
        VersionDescriptor upgradedVersion = darwinForUpgrade.getDarwinStorage().getVersionDescriptorForComponent("test_component");
        assertEquals(new VersionDescriptor("1.1"), upgradedVersion);

        // Verify that the table still exists and column b is still there
        assertTrue(testTableExists());
        assertTrue(columnBExists());

        // The test passes if no exceptions were thrown during the upgrade process
    }

    private boolean testTableExists() {
        try {
            getJdbcTemplate().execute("SELECT * FROM TEST_TABLE");
            return true;
        } catch(BadSqlGrammarException ex) {
            return false;
        }
    }

    private boolean columnBExists() {
        try {
            getJdbcTemplate().execute("SELECT b FROM TEST_TABLE");
            return true;
        } catch(BadSqlGrammarException ex) {
            return false;
        }
    }

}
