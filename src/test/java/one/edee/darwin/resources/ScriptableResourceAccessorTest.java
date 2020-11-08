package one.edee.darwin.resources;

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.resources.ScriptableResourceAccessorTest.TestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(
        classes = TestConfiguration.class
)
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class ScriptableResourceAccessorTest extends AbstractDarwinTest {
	@Autowired private ScriptableResourceAccessor scriptableDbAutoUpdateResourceAccessor;

	@Test
	public void testGetTextContentFromResource() {
		String content = scriptableDbAutoUpdateResourceAccessor.getTextContentFromResource("mysql/scriptedCreate.sql");
		assertNotNull(content);
		assertTrue(content.startsWith("create table MYPREF_DARWIN"));
	}

	@Configuration
	public static class TestConfiguration {

		@Bean
		public ScriptableResourceAccessor scriptableDbAutoUpdateResourceAccessor(ResourceLoader resourceLoader) {
			final ScriptableResourceAccessor scriptableResourceAccessor = new ScriptableResourceAccessor(resourceLoader, "UTF-8", "classpath:/META-INF/darwin/sql-test/upgrade/");
			scriptableResourceAccessor.setVars(
					Collections.singletonMap("tablePrefix", "MYPREF_")
			);
			return scriptableResourceAccessor;
		}

	}

}