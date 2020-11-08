package one.edee.darwin.utils.spring;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * Contains datasource beans.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
@Configuration
public class DataSourceConfiguration {

	@Bean("dataSource")
	@Profile("MYSQL")
	public DataSource mysqlDataSource(
			@Value("${jdbc.url.mysql}") String url,
			@Value("${jdbc.user.mysql}") String user,
			@Value("${jdbc.password.mysql}") String password
	) {
		final HikariConfig cfg = new HikariConfig();

		cfg.setJdbcUrl(url);
		cfg.setUsername(user);
		cfg.setPassword(password);
		cfg.setAutoCommit(true);
		cfg.setMaximumPoolSize(5);
		cfg.setMaxLifetime(10000);
		cfg.addDataSourceProperty( "cachePrepStmts" , "true" );
		cfg.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		cfg.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
		return new TransactionAwareDataSourceProxy(
				new HikariDataSource(cfg)
		);
	}

	@Bean("dataSource")
	@Profile("ORACLE")
	public DataSource oracleDataSource(
			@Value("${jdbc.url.oracle}") String url,
			@Value("${jdbc.user.oracle}") String user,
			@Value("${jdbc.password.oracle}") String password
	) {
		final HikariConfig cfg = new HikariConfig();

		cfg.setJdbcUrl(url);
		cfg.setUsername(user);
		cfg.setPassword(password);
		cfg.setAutoCommit(true);
		cfg.setMaximumPoolSize(5);
		cfg.setMaxLifetime(10000);
		cfg.addDataSourceProperty( "cachePrepStmts" , "true" );
		cfg.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		cfg.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
		return new TransactionAwareDataSourceProxy(
				new HikariDataSource(cfg)
		);
	}

	@Bean("dataSource")
	@Profile("H2")
	public DataSource h2DataSource(
			@Value("${jdbc.url.h2}") String url,
			@Value("${jdbc.user.h2}") String user,
			@Value("${jdbc.password.h2}") String password
	) {
		final HikariConfig cfg = new HikariConfig();

		cfg.setJdbcUrl(url);
		cfg.setUsername(user);
		cfg.setPassword(password);
		cfg.setAutoCommit(true);
		cfg.setMaximumPoolSize(5);
		cfg.setMaxLifetime(10000);
		cfg.addDataSourceProperty( "cachePrepStmts" , "true" );
		cfg.addDataSourceProperty( "prepStmtCacheSize" , "250" );
		cfg.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
		return new TransactionAwareDataSourceProxy(
				new HikariDataSource(cfg)
		);
	}

	@Bean
	public PlatformTransactionManager transactionManager(DataSource dataSourceProxy) {
		return new DataSourceTransactionManager(dataSourceProxy);
	}

	@Bean
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

}
