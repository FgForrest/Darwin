package one.edee.darwin.storage;

import one.edee.darwin.AbstractDarwinTest;
import one.edee.darwin.resources.DefaultResourceAccessor;
import one.edee.darwin.storage.DefaultDatabaseStorageUpdaterTest.TestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * No extra information provided - see (selfexplanatory) method signatures.
 * I have the best intention to write more detailed documentation but if you see this, there was not enough time or will to do so.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
@ContextConfiguration(
		classes = {
				TestConfiguration.class
		}
)
public class DefaultDatabaseStorageUpdaterTest extends AbstractDarwinTest {

	@Autowired
	private DefaultDatabaseStorageUpdater tested;

	@Autowired
	@Qualifier("darwinResourceAccessor4Test")
	private DefaultResourceAccessor alternativeDbDarwinResourceAccessor;

	@Test
	public void shouldRemoveComments() {
		String content = alternativeDbDarwinResourceAccessor.getTextContentFromResource("mysql/commented.sql");
		assertNotNull(content);
		String processedContent = tested.removeCommentsFromContent(content);
		assertTrue(processedContent.contains("select ME from T_DB_DRUNKPEOPLE;"));
		assertTrue(processedContent.contains("select MEASWELL from T_DB_STONED"));
		assertTrue(processedContent.contains("select MEANDMYFRIENDS from T_DB_DRUNKSTONEDPEOPLE;"));
		assertFalse(processedContent.contains("firstComment"));
		assertFalse(processedContent.contains("secondComment"));
		assertFalse(processedContent.contains("fourthComment"));
		assertFalse(processedContent.contains("fifthComment"));
	}

	@Test
	public void shouldRemoveCommentsHashContent() {
		String content = alternativeDbDarwinResourceAccessor.getTextContentFromResource("mysql/commented2.sql");
		assertNotNull(content);
		String processedContent = tested.removeCommentsFromContent(content);
		assertFalse(processedContent.contains("# firstComment"));
		assertFalse(processedContent.contains("-- secondComment"));
		assertFalse(processedContent.contains("/* thirdComment */"));
		assertFalse(processedContent.contains("# fifthComment"));
		assertFalse(processedContent.replace("\r\n", "\n").contains("/*\n" +
				"sixthComment\n" +
				"*/"));
		assertTrue(processedContent.contains("'#E4F0F8'"));
		if (processedContent.replace("\r\n","\n").contains("as\n")) {
			assertTrue(processedContent.replace("\r\n","\n").contains("as\n"));
		} else {
			assertTrue(processedContent.replace("\r\n","\n").contains("as\n"));
		}
	}

	@Configuration
	public static class TestConfiguration {

		@Bean
		public DefaultResourceAccessor darwinResourceAccessor4Test(ResourceLoader resourceLoader) {
			return new DefaultResourceAccessor(resourceLoader, "UTF-8", "classpath:/META-INF/darwin/sql-test/upgrade/");
		}

	}

}
