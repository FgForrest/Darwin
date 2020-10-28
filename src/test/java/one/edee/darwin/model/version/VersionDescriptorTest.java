package one.edee.darwin.model.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Version descriptor parse test.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public class VersionDescriptorTest {

	@Test
	public void testVersionDescriber() {
		String version = "1.0.1";
		checkParse(version, version);

		version = "1";
		checkParse(version, version);

		version = "1.0.0";
		checkParse(version, "1");

		version = "1.0.0.a.b1278";
		checkParse(version, "1.a.b1278");
	}

	@Test
	private void checkParse(String version, String resultVersion) {
		VersionDescriptor descriptor = new VersionDescriptor(version);
		assertEquals(resultVersion, descriptor.toString());
	}

}