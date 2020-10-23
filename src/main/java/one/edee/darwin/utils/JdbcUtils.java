package one.edee.darwin.utils;

import one.edee.darwin.exception.DatabaseConnectionException;
import one.edee.darwin.storage.AbstractDatabaseStorage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public class JdbcUtils {
	private static final Log log = LogFactory.getLog(JdbcUtils.class);
	private static final Map<String, String> CACHED_RESULTS = new ConcurrentHashMap<String, String>();
    private static final String JDBC_DRIVE_NAME_PREFIX = "jdbc:";

    private JdbcUtils() {
    }

    public static String getPlatformFromJdbcUrl(DataSource dataSource) {
        String result = CACHED_RESULTS.get(Integer.toString(dataSource.hashCode()));
		if (result == null) {
			try {
                Connection connection = null;
                try {
					connection = dataSource.getConnection();
					String driverName = connection.getMetaData().getURL();
                    result = getPlatformFromJdbcUrl(driverName);

					if (result != null) {
						if (log.isInfoEnabled()) {
							log.info("Recognized database platform: " + result);
						}
					} else {
						if(log.isErrorEnabled()) {
							log.error("Unrecognized database platform for driver: " + driverName);
						}
					}

					if (result != null) {
						CACHED_RESULTS.put(Integer.toString(dataSource.hashCode()), result);
					}
				}
				finally {
					if (connection != null) {
                        connection.close();
                    }
				}
			}
			catch(SQLException ex) {
				String msg = "Cannot connect to database.";
				log.fatal(msg, ex);
				throw new DatabaseConnectionException(ex);
			}
		} else {
			if (log.isTraceEnabled()) {
				log.trace("Returning previously recognized platform: " + result);
			}
			return result;
		}

		return result;
    }

    private static String getPlatformFromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl.startsWith(JDBC_DRIVE_NAME_PREFIX + AbstractDatabaseStorage.MYSQL)) {
            return AbstractDatabaseStorage.MYSQL;
        }
        if (jdbcUrl.startsWith(JDBC_DRIVE_NAME_PREFIX + AbstractDatabaseStorage.ORACLE)) {
            return AbstractDatabaseStorage.ORACLE;
        }
        if (jdbcUrl.startsWith(JDBC_DRIVE_NAME_PREFIX + AbstractDatabaseStorage.H2)) {
            return AbstractDatabaseStorage.H2;
        }
        if (jdbcUrl.contains(AbstractDatabaseStorage.MYSQL)) {
            return AbstractDatabaseStorage.MYSQL;
        }
        if (jdbcUrl.contains(AbstractDatabaseStorage.ORACLE)) {
            return AbstractDatabaseStorage.ORACLE;
        }
        if (jdbcUrl.contains(AbstractDatabaseStorage.H2)) {
            return AbstractDatabaseStorage.H2;
        }
	    if (jdbcUrl.contains("sqlserver")) {
		    return AbstractDatabaseStorage.MSSQL;
	    }
        return null;
    }
}
