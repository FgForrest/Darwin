package one.edee.darwin.model;

import one.edee.darwin.model.version.VersionComparator;
import one.edee.darwin.model.version.VersionDescriptor;
import one.edee.darwin.resources.DefaultResourceMatcher;
import one.edee.darwin.resources.ResourceMatcher;
import org.springframework.core.io.Resource;

import java.io.Serializable;
import java.util.Comparator;

import static java.util.Optional.ofNullable;

/**
 * Can compare two Resource objects. If resources' file name contains valid patch version as defined in
 * {@link VersionDescriptor} compares resources by {@link VersionComparator} implementation.
 *
 * @author Michal Kolesnac, FG Forrest a.s. (c) 2009
 */
public class ResourceVersionComparator implements Comparator<Resource>, Serializable {
    private static final long serialVersionUID = -7943722730050082245L;
    private final ResourceMatcher resourceMatcher = new DefaultResourceMatcher();
    private final VersionComparator versionComparator = new VersionComparator();

    @Override
    public int compare(Resource o1, Resource o2) {
        if (o1 == null || o2 == null) {
            throw new IllegalArgumentException("Resources to be compared against cannot be null!");
        }

        final VersionDescriptor v1 = resourceMatcher.getVersionFromResource(o1);
        final VersionDescriptor v2 = resourceMatcher.getVersionFromResource(o2);
        if (v1 != null && v2 == null) {
            return 1;
        }
        if (v2 != null) {
            return versionComparator.compare(v1, v2);
        }
        final String aFileName = ofNullable(o1.getFilename()).map(String::toLowerCase).orElse("");
        final String bFileName = ofNullable(o2.getFilename()).map(String::toLowerCase).orElse("");
        int result = aFileName.compareToIgnoreCase(bFileName);
        if (result > 0) {
            return 1;
        }
        if (result < 0) {
            return -1;
        }
        return result;
    }
}
