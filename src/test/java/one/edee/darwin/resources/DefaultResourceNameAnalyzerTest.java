package one.edee.darwin.resources;

import one.edee.darwin.model.Patch;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Radek Salay, FG Forest a.s. 6/24/16.
 */
public class DefaultResourceNameAnalyzerTest {
    private final DefaultResourceNameAnalyzer defaultResourceNameAnalyzer = new DefaultResourceNameAnalyzer();
    private final Patch patch = new Patch(5, "guess_1.5.sql", "test", new Date(), "mySQL");
    private final Resource patch_1_0_1 = new FileSystemResource("mysql/patch_1.0.1.sql");
    private final Resource create = new FileSystemResource("create.sql");

    @Test
    public void getVersionFromResourcePatch() throws Exception {
        assertEquals("1.5",defaultResourceNameAnalyzer.getVersionFromPatch(patch).toString());
    }

    @Test
    public void getPatchNameFromResource() throws Exception {
        assertEquals("patch_1.0.1.sql",defaultResourceNameAnalyzer.getPatchNameFromResource(patch_1_0_1));
        assertEquals("create.sql",defaultResourceNameAnalyzer.getPatchNameFromResource(create));
    }

    @Test
    public void getPlatformAndNameFromResourcePath() throws Exception {
        String[] sql = {"mysql","patch_1.0.6.sql"};
        assertArrayEquals(sql, defaultResourceNameAnalyzer.getPlatformAndNameFromResourcePath("mysql/patch_1.0.6.sql"));
    }

}