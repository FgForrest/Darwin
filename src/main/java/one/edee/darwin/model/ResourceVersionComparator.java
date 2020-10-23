package one.edee.darwin.model;

import com.fg.commons.version.VersionComparator;
import com.fg.commons.version.VersionDescriptor;
import one.edee.darwin.resources.DefaultResourceNameAnalyzer;
import one.edee.darwin.resources.ResourceNameAnalyzer;
import org.springframework.core.io.Resource;

import java.io.Serializable;

/**
 * Can compare two Resource object. If resource's file name contains version in accord with
 * {@link VersionDescriptor} compare
 * resources by {@link VersionComparator}.
 *
 * @author Michal Kolesnac, FG Forrest a.s. (c) 2009
 * @version $Id$
 */
public class ResourceVersionComparator extends VersionComparator implements Serializable {
    private static final long serialVersionUID = -7943722730050082245L;
    private final ResourceNameAnalyzer resourceNameAnalyzer = new DefaultResourceNameAnalyzer();

    @Override
    public int compare(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            throw new IllegalArgumentException("Resources to be compared against cannot be null!");
        }
        Resource a = (Resource) o1;
        Resource b = (Resource) o2;

        VersionDescriptor v1 = resourceNameAnalyzer.getVersionFromResource(a);
        VersionDescriptor v2 = resourceNameAnalyzer.getVersionFromResource(b);
        if (v1 != null && v2 == null) {
            return 1;
        }
        if (v2 != null) {
            return super.compare(v1, v2);
        }
        int result = a.getFilename().compareToIgnoreCase(b.getFilename());
        if (result > 0) {
            return 1;
        }
        if (result < 0) {
            return -1;
        }
        return result;
    }
}
