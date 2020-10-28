package one.edee.darwin.resources;

import one.edee.darwin.model.Patch;
import one.edee.darwin.model.version.VersionDescriptor;
import org.springframework.core.io.Resource;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default resource name version extractor.
 * Version patches names are expected to be in form of: patch_version.extension
 * For example:
 *
 * patch_1.0.0.sql
 * patch_2.0.sql
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public class DefaultResourceNameAnalyzer implements ResourceNameAnalyzer {
    private static final long serialVersionUID = 3329089530167442837L;
    private static final Pattern VERSION_PATTERN = Pattern.compile("_(.*).sql$");
    private static final Pattern PLATFORM_AND_NAME_PATTERN = Pattern.compile("(.*)/(.*)");

    @Override
    public VersionDescriptor getVersionFromResource(Resource resource) {
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

    @Override
    public VersionDescriptor getVersionFromPatch(Patch patch) {
        final Matcher matcher = VERSION_PATTERN.matcher(patch.getPatchName());
        if (matcher.find()) {
            return new VersionDescriptor(matcher.group(1));
        }
        return null;
    }

    @Override
    public String getPatchNameFromResource(Resource resource) {
            return resource.getFilename();
    }

    @Override
    public String[] getPlatformAndNameFromResourcePath(String resourcePath) {
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
