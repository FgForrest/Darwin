package one.edee.darwin.integrate;

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.DarwinBuilder;
import one.edee.darwin.spring.DarwinConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Radek Salay, FG Forest a.s. 7/11/16.
 */
@ContextConfiguration(
		classes = {
				DarwinConfiguration.class
		}
)
@ActiveProfiles("MYSQL")
public class IntegrationTestDuplicateCommands extends AbstractDarwinTest {
	@Autowired private ApplicationContext applicationContext;
	@Autowired private JdbcTemplate jdbcTemplate;

	@Test
	public void IntegrationTest_LowerPatchSuddenlyAppears_andIsRetrospectivelyApplied() {
		new DarwinBuilder(applicationContext, "duplicate", "1.1")
				.withResourcePath("/META-INF/darwin/sql-test/occurence/correct/")
				.build()
				.evolve();

		// there should be
		assertEquals(Integer.valueOf(-1), jdbcTemplate.queryForObject("select * from TEST", Integer.class));
	}

	@AfterEach
	public void tearDown() throws Exception {
		jdbcTemplate.update("DROP TABLE IF EXISTS TEST");
		jdbcTemplate.update("DELETE FROM DARWIN WHERE component = ?", "duplicate");
	}
}
