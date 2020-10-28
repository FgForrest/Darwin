package one.edee.darwin.integrate;

import one.edee.darwin.AbstractDbAutoupdateTest;
import one.edee.darwin.DarwinBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Radek Salay, FG Forest a.s. 7/11/16.
 */
@ContextConfiguration(
		locations = {
				"/META-INF/darwin/spring/datasource-config.xml",
				"/META-INF/darwin/spring/db-autoupdate-config.xml"
		}
)
@ActiveProfiles("MYSQL")
public class IntegrationTestTransactionBoundaryRespecting extends AbstractDbAutoupdateTest {
	@Autowired private ApplicationContext applicationContext;
	@Autowired @Qualifier(value = "jdbcTemplateTest") private JdbcTemplate jdbcTemplate;

	@Test
	public void IntegrationTest_LowerPatchSuddenlyAppears_andIsRetrospectivelyApplied() throws Exception {
		try {
			// this will apply init variables and stops on ERROR!
			new DarwinBuilder(applicationContext, "duplicate", "1.1")
					.withResourcePath("/META-INF/darwin/sql-test/occurence/wrong/")
					.build()
					.apply();
		} catch (BadSqlGrammarException ignored) {
			// this is expected
		}

		// this will try to continue after ERROR! has been removed but it needs to reinit variables that
		// successfully passed in first run
		new DarwinBuilder(applicationContext, "duplicate", "1.1")
				.withResourcePath("/META-INF/darwin/sql-test/occurence/correct/")
				.build()
				.apply();

		// there should be
		assertEquals(Integer.valueOf(-1), jdbcTemplate.queryForObject("select * from TEST", Integer.class));
	}

	@AfterEach
	public void tearDown() throws Exception {
		jdbcTemplate.update("DROP TABLE IF EXISTS TEST");
		jdbcTemplate.update("DELETE FROM T_DB_AUTOUPDATE WHERE COMPONENT_TX = ?", "duplicate");
	}
}
