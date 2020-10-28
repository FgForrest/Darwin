package one.edee.darwin.integrate;

import one.edee.darwin.AbstractDbAutoupdateTest;
import one.edee.darwin.Darwin;
import one.edee.darwin.model.SchemaVersion;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.utils.AutoupdateTestHelper;
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
public abstract class IntegrationTestCreateDbStructureFromNothing extends AbstractDbAutoupdateTest {
	@Autowired ApplicationContext applicationContext;
    @Autowired Darwin darwin;

    /**
     * this test is about creating DB structure from state where don't exit any table. On end {@link Darwin#afterPropertiesSet()} exist complete DB structure.
     *
     * @throws Exception
     */
    @Test
    public void IntegrationTest_EmptyDatabase_AutoUpdaterInitializesItsDataStructureEntirely() throws Exception {
    	//clean entire DB
		AutoupdateTestHelper.deleteAllInfrastructuralPages(getJdbcTemplate());
		//patch and SQL tables should not exists
		assertFalse(existsPatchAndSqlTableNoCache());
		//run autoupdater
		darwin.afterPropertiesSet();
		//check patch and SQL table exists
        assertTrue(existsPatchAndSqlTableNoCache());
    }

	/**
	 * this test is about creating DB structure from state where don't exit any table. On end {@link Darwin#afterPropertiesSet()} exist complete DB structure.
	 *
	 * @throws Exception
	 */
	@Test
	public void IntegrationTest_EmptyDatabase_AutoUpdaterInitializesItsDataStructureEntirelyFromDefaultConfiguration() throws Exception {
		Darwin darwin = new Darwin();
		darwin.setApplicationContext(applicationContext);
		darwin.setComponentDescriptor(new SchemaVersion("test_component", "1.0"));
		darwin.setResourceAccessor(new DefaultResourceAccessor(applicationContext, "UTF-8", "classpath:/com/fg/autoupdate/utils/"));
		//clean entire DB
		AutoupdateTestHelper.deleteAllInfrastructuralPages(getJdbcTemplate());
		//patch and SQL tables should not exists
		assertFalse(existsPatchAndSqlTableNoCache());
		//run autoupdater
		darwin.afterPropertiesSet();
		//check patch and SQL table exists
		assertTrue(existsPatchAndSqlTableNoCache());
	}

	private boolean existsPatchAndSqlTableNoCache() {
		try {
			getJdbcTemplate().execute("SELECT * FROM T_DB_AUTOUPDATE_PATCH");
			getJdbcTemplate().execute("SELECT * FROM T_DB_AUTOUPDATE_SQL");
			return true;
		} catch(BadSqlGrammarException ex) {
			return false;
		}
	}

	@AfterEach
    public void tearDown() throws Exception {
		AutoupdateTestHelper.deleteAllInfrastructuralPages(getJdbcTemplate());
    }

}
