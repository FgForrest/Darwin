package one.edee.darwin.config;

import one.edee.darwin.Darwin;
import one.edee.darwin.locker.Locker;
import one.edee.darwin.spring.DarwinConfiguration;
import one.edee.darwin.utils.spring.DarwinTestRequirementsConfiguration;
import one.edee.darwin.utils.spring.DataSourceConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
		classes = {
				DataSourceConfiguration.class,
				DarwinTestRequirementsConfiguration.class,
				DarwinConfiguration.class
		}
)
@ActiveProfiles(value = "MYSQL")
public class JavaConfigTest {
	@Autowired private Darwin darwin;
	@Autowired private Locker locker;

	@Test
	public void JavaConfig_Setup_Ok() {
		assertNotNull(darwin);
		assertNotNull(locker);
	}

}
