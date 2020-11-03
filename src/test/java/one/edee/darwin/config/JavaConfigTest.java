package one.edee.darwin.config;

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
	classes = DarwinConfiguration.class
)
public class JavaConfigTest {
	@Autowired private Darwin darwin;
	@Autowired private Locker locker;
	@Autowired private ResourceAccessor dbResourceAccessor;

	@Test
	public void JavaConfig_Setup_Ok() {
		assertNotNull(darwin);
		assertNotNull(locker);
		assertNotNull(dbResourceAccessor);
	}

}
