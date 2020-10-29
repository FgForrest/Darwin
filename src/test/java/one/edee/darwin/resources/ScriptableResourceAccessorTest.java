package one.edee.darwin.resources;

import one.edee.darwin.AbstractDarwinTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ContextConfiguration(
        locations = {"classpath:com/fg/autoupdate/resources/scriptableResource-config.xml"}
)
@ActiveProfiles(value = "MYSQL")
@Profile(value = "MYSQL")
public class ScriptableResourceAccessorTest extends AbstractDarwinTest {
	@Autowired private ScriptableResourceAccessor scriptableDbAutoUpdateResourceAccessor;

	@Test
	public void testGetTextContentFromResource() {
		String content = scriptableDbAutoUpdateResourceAccessor.getTextContentFromResource("sql-test/upgrade/mysql/scriptedCreate.sql");
		assertNotNull(content);
		assertTrue(content.startsWith("create table T_DB_MYPREF_AUTOUPDATE"));
	}
}