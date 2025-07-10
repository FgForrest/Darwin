package one.edee.darwin.resources;

import lombok.NonNull;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.version.VersionDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation of resource matcher. File names must follow specific regex patterns for each {@link PatchType}.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public class DefaultResourceMatcher implements ResourceMatcher {
	private static final Pattern PLATFORM_AND_NAME_PATTERN = Pattern.compile("(.*)/(.*)");

	// Regex patterns for each patch type
	private static final Pattern CREATE_PATTERN = Pattern.compile("^create\\.sql$", Pattern.CASE_INSENSITIVE);
	// Version pattern: must start with alphanumeric, no consecutive dots/hyphens/underscores, no trailing dots/hyphens/underscores
	private static final Pattern EVOLVE_PATTERN = Pattern.compile("^patch_([a-zA-Z0-9]+(?:[._-][a-zA-Z0-9]+)*)\\.sql$", Pattern.CASE_INSENSITIVE);
	private static final Pattern GUESS_PATTERN = Pattern.compile("^guess_([a-zA-Z0-9]+(?:[._-][a-zA-Z0-9]+)*)\\.sql$", Pattern.CASE_INSENSITIVE);

	@Override
	public boolean isResourceAcceptable(@NonNull PatchType type, @NonNull String patchName) {
		// Handle edge cases that tests expect to return false
		if (patchName.isEmpty()) {
			return false;
		}

		switch(type) {
			case CREATE: return matchesPattern(CREATE_PATTERN, patchName);
			case EVOLVE: return matchesPatternWithValidVersion(EVOLVE_PATTERN, patchName);
			case GUESS: return matchesPatternWithValidVersion(GUESS_PATTERN, patchName);
		}

		return false;
	}

	/**
	 * Helper method to check if a patch name matches a given pattern.
	 */
	private boolean matchesPattern(@NonNull Pattern pattern, @NonNull String patchName) {
		return pattern.matcher(patchName).matches();
	}

	/**
	 * Helper method to check if a patch name matches a pattern and contains a valid version.
	 * The version is validated by attempting to create a VersionDescriptor from it.
	 */
	private boolean matchesPatternWithValidVersion(@NonNull Pattern pattern, @NonNull String patchName) {
		final Matcher matcher = pattern.matcher(patchName);
		if (matcher.matches()) {
			final String version = matcher.group(1);
			try {
				// Validate version by attempting to create VersionDescriptor
				new VersionDescriptor(version);
				return true;
			} catch (Exception e) {
				// Invalid version format
				return false;
			}
		}
		return false;
	}

	@Nullable
	@Override
	public VersionDescriptor getVersionFromResource(@NonNull Resource resource) {
		final String fileName = Objects.requireNonNull(resource.getFilename()).toLowerCase();
		int startIndex = fileName.lastIndexOf('_');
		if (startIndex > -1) {
			int index = fileName.lastIndexOf('.');
			if (index > -1 && fileName.length() > 6) {
				String version = fileName.substring(startIndex, index);
				return new VersionDescriptor(version);
			}
		}
		return null;
	}

	@Nullable
	@Override
	public VersionDescriptor getVersionFromPatch(@NonNull Patch patch) {
		final Matcher evolvePattern = EVOLVE_PATTERN.matcher(patch.getPatchName());
		if (evolvePattern.matches()) {
			return new VersionDescriptor(evolvePattern.group(1));
		}
		final Matcher guessPattern = GUESS_PATTERN.matcher(patch.getPatchName());
		if (guessPattern.matches()) {
			return new VersionDescriptor(guessPattern.group(1));
		}
		return null;
	}

	@NonNull
	@Override
	public String getPatchNameFromResource(@NonNull Resource resource) {
		return Objects.requireNonNull(resource.getFilename());
	}

	@NonNull
	@Override
	public String[] getPlatformAndNameFromResourcePath(@NonNull String resourcePath) {
		final Matcher matcher = PLATFORM_AND_NAME_PATTERN.matcher(resourcePath);
		if (matcher.find()) {
			return new String[] {
				matcher.group(1),
				matcher.group(2)
			};
		}
		return null;
	}

}
