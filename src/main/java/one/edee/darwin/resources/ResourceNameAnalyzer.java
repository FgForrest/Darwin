package one.edee.darwin.resources;

import one.edee.darwin.model.Patch;
import one.edee.darwin.model.version.VersionDescriptor;
import org.springframework.core.io.Resource;

import java.io.Serializable;

/**
 * Extract version information from specified resource.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface ResourceNameAnalyzer extends Serializable {

    /**
     * Analyzes name of patch represented as {@link Resource} and extracts version from its name.
     * @param resource from we want version
     * @return return version of resource example patch_<b>1.2</b>.sql, if hasn't version, example create.sql then return null.
     */
    VersionDescriptor getVersionFromResource(Resource resource);

    /**
     * Analyzes name of patch and extracts version from its name.
     * @param patch from we want version
     * @return return version of resource example patch_<b>1.2</b>.sql, if hasn't version, example create.sql then return null.
     */
    VersionDescriptor getVersionFromPatch(Patch patch);

    /**
     * Extracts name from patch represented as {@link Resource}.
     * @param resource from which we want name
     * @return name of current resource, example patch_1.5.sql
     */
    String getPatchNameFromResource(Resource resource);

    /**
     * Extracts platform and name from patch represented as {@link Resource}.
     * @param resourcePath resourcePath in example mysql/patch_1.2.sql
     * @return return array and on 0 index is platform(mysql) and on 1 is patchName(patch_1.2.sql)
     */
    String[] getPlatformAndNameFromResourcePath(String resourcePath);

}
