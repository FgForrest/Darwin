package one.edee.darwin.config;

/**
 * No documentation needed, just look at the methods.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2016
 */

import com.fg.support.test.AbstractSpringTestCase;
import com.fg.support.test.AnnotationHostConfigurableContextLoader;
import one.edee.darwin.Darwin;
import one.edee.darwin.locker.Locker;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.spring.DarwinConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(
	classes = DarwinConfiguration.class,
	loader = AnnotationHostConfigurableContextLoader.class
)
public class JavaConfigTest extends AbstractSpringTestCase {
	@Autowired private Darwin dbDarwin;
	@Autowired private Locker locker;
	@Autowired private ResourceAccessor dbResourceAccessor;

	@Test
	public void JavaConfig_Setup_Ok() throws Exception {
		assertNotNull(dbDarwin);
		assertNotNull(locker);
		assertNotNull(dbResourceAccessor);
	}

}
