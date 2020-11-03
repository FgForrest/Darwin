package one.edee.darwin;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        locations = {
		        "/META-INF/darwin/spring/datasource-config.xml",
		        "/META-INF/darwin/spring/db-autoupdate-config-test.xml"
        }
)
@PropertySource("classpath:test.properties")
public abstract class AbstractDarwinTest {
	@Autowired private DataSource dataSource;

	protected JdbcTemplate getJdbcTemplate() {
		return new JdbcTemplate(dataSource);
	}

}
