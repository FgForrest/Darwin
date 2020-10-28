package one.edee.darwin.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.exception.DatabaseConnectionException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Represents platform of the RDBMS.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
@RequiredArgsConstructor
@CommonsLog
public enum Platform {

	MYSQL(Pattern.compile("jdbc:mysql:.*"), "mysql"),
	ORACLE(Pattern.compile("jdbc:oracle:.*"), "oracle"),
	MSSQL(Pattern.compile("jdbc:sqlserver:.*"), "mssql"),
	H2(Pattern.compile("jdbc:h2:.*"), "h2");

	private static final Map<String, Platform> CACHED_RESULTS = new ConcurrentHashMap<>();
	private static final String JDBC_DRIVE_NAME_PREFIX = "jdbc:";

	@Getter private final Pattern jdbcUrlPattern;
	@Getter private final String folderName;

	public static Platform getPlatformFromJdbcUrl(DataSource dataSource) {
		Platform result = CACHED_RESULTS.get(Integer.toString(dataSource.hashCode()));
		if (result == null) {
			try {
				try (final Connection connection = dataSource.getConnection()) {
					final String driverName = connection.getMetaData().getURL();
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
			} catch(SQLException ex) {
				final String msg = "Cannot connect to database.";
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

	private static Platform getPlatformFromJdbcUrl(String jdbcUrl) {
		for (Platform platform : Platform.values()) {
			if (platform.getJdbcUrlPattern().matcher(jdbcUrl).matches()) {
				return platform;
			}
		}
		return null;
	}

	public static Platform identify(String platform) {
		for (Platform platformEnum : Platform.values()) {
			if (platformEnum.name().equals(platform) || platformEnum.getFolderName().equals(platform)) {
				return platformEnum;
			}
		}
		throw new IllegalArgumentException("Platform " + platform + " not recognized!");
	}
}
