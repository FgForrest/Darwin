package one.edee.darwin.storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Contains helper methods to read data from input stream.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
public interface IOUtils {

	/**
	 * Reads input stream to string using specified charset.
	 * @param inputStream
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	default String toString(InputStream inputStream, Charset charset) throws IOException {
		final StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charset))) {
			String line;
			while ((line = br.readLine()) != null) {
				resultStringBuilder.append(line).append("\n");
			}
		}
		return resultStringBuilder.toString();
	}

}
