package one.edee.darwin;

import com.fg.support.test.AbstractSpringTestCase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;

/**
 * Description
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
@ContextConfiguration(
        locations = {
                "/META-INF/lib_db_autoupdate/spring/datasource-config.xml",
                "/META-INF/lib_db_autoupdate/spring/db-autoupdate-config-test.xml"
        }
)
public abstract class AbstractDbAutoupdateTest extends AbstractSpringTestCase {
	@Autowired private DataSource dataSource;

	protected JdbcTemplate getJdbcTemplate() {
		return new JdbcTemplate(dataSource);
	}

}
