package one.edee.darwin.resources;

import one.edee.darwin.model.Patch;
import one.edee.darwin.model.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultResourceMatcher to verify regex pattern matching for different patch types.
 *
 * author Jan Novotný, FG Forrest a.s. (c) 2025
 */
public class DefaultResourceMatcherTest {
    private DefaultResourceMatcher matcher;
    private final Patch patch = new Patch(5, "guess_1.5.sql", "test", LocalDateTime.now(), Platform.MYSQL);
    private final Resource patch_1_0_1 = new FileSystemResource("mysql/patch_1.0.1.sql");
    private final Resource create = new FileSystemResource("create.sql");

    @BeforeEach
    public void setUp() {
        matcher = new DefaultResourceMatcher();
    }

    @Test
    public void getVersionFromResourcePatch() {
        assertEquals("1.5", matcher.getVersionFromPatch(patch).toString());
    }

    @Test
    public void getPatchNameFromResource() {
        assertEquals("patch_1.0.1.sql", matcher.getPatchNameFromResource(patch_1_0_1));
        assertEquals("create.sql", matcher.getPatchNameFromResource(create));
    }

    @Test
    public void getPlatformAndNameFromResourcePath() {
        String[] sql = {"mysql","patch_1.0.6.sql"};
        assertArrayEquals(sql, matcher.getPlatformAndNameFromResourcePath("mysql/patch_1.0.6.sql"));
    }

    @Test
    public void testCreatePatternMatching() {
        // Valid CREATE patterns
        assertTrue(matcher.isResourceAcceptable(PatchType.CREATE, "create.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.CREATE, "CREATE.SQL"));
        assertTrue(matcher.isResourceAcceptable(PatchType.CREATE, "Create.Sql"));

        // Invalid CREATE patterns
        assertFalse(matcher.isResourceAcceptable(PatchType.CREATE, "create"));
        assertFalse(matcher.isResourceAcceptable(PatchType.CREATE, "create.txt"));
        assertFalse(matcher.isResourceAcceptable(PatchType.CREATE, "create_1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.CREATE, "patch_create.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.CREATE, "createtable.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.CREATE, "create.sql.backup"));
    }

    @Test
    public void testEvolvePatternMatching() {
        // Valid EVOLVE patterns with numeric versions
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0.0.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.2.3.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "PATCH_1.SQL"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "Patch_1.0.Sql"));

        // Valid EVOLVE patterns with version suffixes
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0-SNAPSHOT.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0-alpha.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0-beta.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0-RC.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0-RELEASE.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0-alpha-1.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0-alpha-20.sql"));

        // Valid EVOLVE patterns with underscore and hyphen delimiters
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1_0_0.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1-0-0.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0_1-alpha.sql"));

        // Valid EVOLVE patterns with alphanumeric versions
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_a.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_a.b.c.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_v1.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_version1.sql"));

        // Invalid EVOLVE patterns
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.txt"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch-1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1..sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0..sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0-.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_.1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.sql.backup"));
    }

    @Test
    public void testGuessPatternMatching() {
        // Valid GUESS patterns with numeric versions
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0.0.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.2.3.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "GUESS_1.SQL"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "Guess_1.0.Sql"));

        // Valid GUESS patterns with version suffixes
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0-SNAPSHOT.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0-alpha.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0-beta.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0-RC.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0-RELEASE.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0-alpha-1.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0-alpha-20.sql"));

        // Valid GUESS patterns with underscore and hyphen delimiters
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1_0_0.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1-0-0.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0_1-alpha.sql"));

        // Valid GUESS patterns with alphanumeric versions
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_a.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_a.b.c.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_v1.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_version1.sql"));

        // Invalid GUESS patterns
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess_.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.txt"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess-1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1..sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0..sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.0-.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess_.1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.sql.backup"));
    }

    @Test
    public void testEdgeCases() {
        // Test empty and null strings
        assertFalse(matcher.isResourceAcceptable(PatchType.CREATE, ""));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, ""));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, ""));

        // Test cross-type matching (should not match)
        assertFalse(matcher.isResourceAcceptable(PatchType.CREATE, "patch_1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.CREATE, "guess_1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "create.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.EVOLVE, "guess_1.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "create.sql"));
        assertFalse(matcher.isResourceAcceptable(PatchType.GUESS, "patch_1.sql"));
    }

    @Test
    public void testComplexVersionFormats() {
        // Test complex version formats that VersionDescriptor supports
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.8.9.1-alpha.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.8-RC1.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_2.0.0-SNAPSHOT.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.2.2-RELEASE.sql"));

        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.8.9.1-alpha.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.8-RC1.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_2.0.0-SNAPSHOT.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_1.2.2-RELEASE.sql"));

        // Test mixed alphanumeric versions
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_1.0.0.a.b1278.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_e.a.b.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.EVOLVE, "patch_a.a.a.sql"));
        assertTrue(matcher.isResourceAcceptable(PatchType.GUESS, "guess_f.sql"));
    }

}
