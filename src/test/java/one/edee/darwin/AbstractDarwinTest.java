package one.edee.darwin;

import one.edee.darwin.utils.spring.DarwinTestRequirementsConfiguration;
import one.edee.darwin.utils.spring.DataSourceConfiguration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
        classes = {
		        DataSourceConfiguration.class,
		        DarwinTestRequirementsConfiguration.class
        }
)
public abstract class AbstractDarwinTest {
	@Autowired private DataSource dataSource;

	protected JdbcTemplate getJdbcTemplate() {
		return new JdbcTemplate(dataSource);
	}

}
