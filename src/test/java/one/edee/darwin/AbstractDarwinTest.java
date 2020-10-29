package one.edee.darwin;

import com.fg.support.test.AbstractSpringTestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;

@ContextConfiguration(
        locations = {
		        "/META-INF/darwin/spring/datasource-config.xml",
		        "/META-INF/darwin/spring/db-autoupdate-config-test.xml"
        }
)
public abstract class AbstractDarwinTest extends AbstractSpringTestCase {
	@Autowired private DataSource dataSource;

	protected JdbcTemplate getJdbcTemplate() {
		return new JdbcTemplate(dataSource);
	}

}
