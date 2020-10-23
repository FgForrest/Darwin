package one.edee.darwin.model;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link ResourceVersionComparator}
 *
 * @author Michal Kolesnac, FG Forrest a.s. (c) 2009
 * @version $Id$
 */
public class ResourceVersionComparatorTest {
    final Resource create = new FileSystemResource("create.sql");
    final Resource insert = new FileSystemResource("insert_component.sql");
    final Resource lock_delete = new FileSystemResource("lock_delete.sql");

    final Resource patch_1_0_1 = new FileSystemResource("patch_1.0.1.sql");
    final Resource patch_1_0_2 = new FileSystemResource("patch_1.0.2.sql");
    final Resource patch_1_0_11 = new FileSystemResource("patch_1.0.11.sql");

    final Resource guess_1_0_1 = new FileSystemResource("guess_1.0.1.sql");
    final Resource guess_1_1_0 = new FileSystemResource("guess_1.1.0.sql");

	ResourceVersionComparator resourceVersionComparator = new ResourceVersionComparator();

	@Test
	public void testCompareResources() throws Exception {
		assertEquals(-1,resourceVersionComparator.compare(create,patch_1_0_1));
		assertEquals(1,resourceVersionComparator.compare(patch_1_0_11,lock_delete));
		assertEquals(0,resourceVersionComparator.compare(create,create));

		assertEquals(-1,resourceVersionComparator.compare(patch_1_0_2,patch_1_0_11));
		assertEquals(1,resourceVersionComparator.compare(patch_1_0_11,patch_1_0_2));
		assertEquals(-1,resourceVersionComparator.compare(patch_1_0_1,patch_1_0_2));
		assertEquals(1,resourceVersionComparator.compare(patch_1_0_11,patch_1_0_1));
		assertEquals(0,resourceVersionComparator.compare(patch_1_0_1,patch_1_0_1));

		assertEquals(-1,resourceVersionComparator.compare(create,lock_delete));
		assertEquals(1,resourceVersionComparator.compare(lock_delete,create));
		assertEquals(-1,resourceVersionComparator.compare(create,insert));
		assertEquals(1,resourceVersionComparator.compare(insert,create));

		assertEquals(0,resourceVersionComparator.compare(patch_1_0_1,guess_1_0_1));

		assertEquals(1,resourceVersionComparator.compare(guess_1_1_0,guess_1_0_1));
		assertEquals(-1,resourceVersionComparator.compare(guess_1_0_1,guess_1_1_0));
	}
}
