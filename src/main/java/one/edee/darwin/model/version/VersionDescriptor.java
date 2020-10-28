package one.edee.darwin.model.version;

import lombok.Getter;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Holds version of specific library.
 * Can work with infinite number of subversion numbering.
 * Can work either with alfa or numeric versioning systems.
 *
 * Examples of such versions:
 *
 * 1.0.0-SNAPSHOT
 * 1.2.2-RELEASE
 * 1.0
 * 1.8.9.1-alpha
 * 1.8-RC1
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public class VersionDescriptor {
	private static final String VERSION_DELIMITER = ".";
	private static final String VERSION_DELIMITERS = "._-";
	private static final String SNAPSHOT_POSTFIX = "SNAPSHOT";

	@Getter private final List<Serializable> identification = new LinkedList<>();
	@Getter private boolean snapshot;

    public VersionDescriptor(String fullVersion) {
		for(StringTokenizer st = new StringTokenizer(fullVersion, VERSION_DELIMITERS, false); st.hasMoreTokens();) {
			final String version = st.nextToken();
			try {
                //ignore snapshot postfix
                if (!SNAPSHOT_POSTFIX.equals(version))   {
					// parse value
					int numberVersion = Integer.parseInt(version);
					identification.add(numberVersion);
				} else {
                	snapshot = true;
                }
			} catch(NumberFormatException ex) {
				identification.add(version);
			}
		}

		int lastIndex = identification.size();
		for(int i = identification.size() - 1; i >= 0; i--) {
			final Object version = identification.get(i);
			if (version instanceof Integer) {
				if ((Integer) version != 0) {
	                lastIndex = i + 1;
					break;
				}
			}
		}

		for(int i = lastIndex; i < identification.size();) {
			if (identification.get(i) instanceof Integer) {
				identification.remove(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * Adds numeric version number to existing version.
	 * @param version
	 * @return
	 */
	public VersionDescriptor addNumericVersion(int version) {
		identification.add(version);
		return this;
	}

	/**
	 * Adds textual version number to existing version.
	 * @param version
	 * @return
	 */
	public VersionDescriptor addAlphanumericVersion(String version) {
		identification.add(version);
		return this;
	}

	public boolean equals(Object o) {
		if(this == o) return true;
		if(o == null || getClass() != o.getClass()) return false;

		final VersionDescriptor that = (VersionDescriptor)o;
		return identification.equals(that.identification);
	}

	public int hashCode() {
		return identification.hashCode();
	}

	public String toString() {
		final StringBuilder fullVersion = new StringBuilder();
		for(int i = 0; i < identification.size(); i++) {
			Object version = identification.get(i);
			fullVersion.append(version);
			if(i < identification.size() - 1) fullVersion.append(VERSION_DELIMITER);
		}

		return fullVersion.toString();
	}

}