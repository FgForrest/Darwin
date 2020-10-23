package one.edee.darwin.utils;
/**
 * Description
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */

import one.edee.darwin.AbstractDbAutoupdateTest;
import one.edee.darwin.storage.AbstractDatabaseStorage;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class JdbcUtilsTest extends AbstractDbAutoupdateTest {
	@Autowired private DataSource dataSource;

	private String jdbcUrl;

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public void testGetPlatformFromJdbcUrl() throws Exception {
		driverAssertion(AbstractDatabaseStorage.MYSQL);
		driverAssertion(AbstractDatabaseStorage.ORACLE);
		driverAssertion(AbstractDatabaseStorage.H2);
	}

	private void driverAssertion(String driverType) {
		if(jdbcUrl.contains(driverType)) {
			 assertEquals(driverType, JdbcUtils.getPlatformFromJdbcUrl(dataSource));
		}
	}

}