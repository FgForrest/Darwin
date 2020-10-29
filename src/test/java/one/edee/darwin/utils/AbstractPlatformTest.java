package one.edee.darwin.utils;
/**
 * Description
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.model.Platform;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;

import static one.edee.darwin.model.Platform.getPlatformFromJdbcUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractPlatformTest extends AbstractDarwinTest {
	@Autowired private DataSource dataSource;

	protected void assertPlatform(Platform currentPlatform) {
		assertEquals(currentPlatform, getPlatformFromJdbcUrl(dataSource));
	}

}