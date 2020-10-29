package one.edee.darwin.integrate;

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.Darwin;
import one.edee.darwin.model.SchemaVersion;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.utils.DarwinTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.test.annotation.DirtiesContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Radek Salay, FG Forest a.s. 6/24/16.
 */
@DirtiesContext
public abstract class IntegrationTestCreateDbStructureFromNothing extends AbstractDarwinTest {
	@Autowired ApplicationContext applicationContext;
    @Autowired Darwin darwin;

    /**
     * this test is about creating DB structure from state where don't exit any table. On end {@link Darwin#afterPropertiesSet()} exist complete DB structure.
     *
     */
    @Test
    public void IntegrationTest_EmptyDatabase_DarwinInitializesItsDataStructureEntirely() {
    	//clean entire DB
		DarwinTestHelper.deleteAllInfrastructuralPages(getJdbcTemplate());
		//patch and SQL tables should not exists
		assertFalse(existsPatchAndSqlTableNoCache());
		//run Darwin
		darwin.afterPropertiesSet();
		//check patch and SQL table exists
        assertTrue(existsPatchAndSqlTableNoCache());
    }

	/**
	 * this test is about creating DB structure from state where don't exit any table. On end {@link Darwin#afterPropertiesSet()} exist complete DB structure.
	 *
	 */
	@Test
	public void IntegrationTest_EmptyDatabase_DarwinInitializesItsDataStructureEntirelyFromDefaultConfiguration() {
		Darwin darwin = new Darwin();
		darwin.setApplicationContext(applicationContext);
		darwin.setComponentDescriptor(new SchemaVersion("test_component", "1.0"));
		darwin.setResourceAccessor(new DefaultResourceAccessor(applicationContext, "UTF-8", "classpath:/com/fg/autoupdate/utils/"));
		//clean entire DB
		DarwinTestHelper.deleteAllInfrastructuralPages(getJdbcTemplate());
		//patch and SQL tables should not exists
		assertFalse(existsPatchAndSqlTableNoCache());
		//run Darwin
		darwin.afterPropertiesSet();
		//check patch and SQL table exists
		assertTrue(existsPatchAndSqlTableNoCache());
	}

	private boolean existsPatchAndSqlTableNoCache() {
		try {
			getJdbcTemplate().execute("SELECT * FROM DARWIN_PATCH");
			getJdbcTemplate().execute("SELECT * FROM DARWIN_SQL");
			return true;
		} catch(BadSqlGrammarException ex) {
			return false;
		}
	}

	@AfterEach
    public void tearDown() {
		DarwinTestHelper.deleteAllInfrastructuralPages(getJdbcTemplate());
    }

}
