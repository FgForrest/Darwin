package one.edee.darwin.resources;

import lombok.NonNull;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.version.VersionDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

/**
 * Implementation of this interface filters patches according their {@link PatchType}.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
public interface ResourceMatcher {

	/**
	 * This method compare patch if he corresponds with presumed type.
	 *
	 * @param type type of patch we presumed.
	 * @param patchName file name of the patch
	 * @return return true if is patch corresponds type, or false if not
	 * @see PatchType
	 */
    boolean isResourceAcceptable(@NonNull PatchType type, @NonNull String patchName);

	/**
	 * Analyzes name of patch represented as {@link Resource} and extracts version from its name.
	 * @param resource from we want version
	 * @return return version of resource example patch_<b>1.2</b>.sql, if hasn't version, example create.sql then return null.
	 */
	@Nullable
	VersionDescriptor getVersionFromResource(@NonNull Resource resource);

	/**
	 * Analyzes name of patch and extracts version from its name.
	 * @param patch from we want version
	 * @return return version of resource example patch_<b>1.2</b>.sql, if hasn't version, example create.sql then return null.
	 */
	@Nullable
	VersionDescriptor getVersionFromPatch(@NonNull Patch patch);

	/**
	 * Extracts name from patch represented as {@link Resource}.
	 * @param resource from which we want name
	 * @return name of current resource, example patch_1.5.sql
	 */
	@NonNull
	String getPatchNameFromResource(@NonNull Resource resource);

	/**
	 * Extracts platform and name from patch represented as {@link Resource}.
	 * @param resourcePath resourcePath in example mysql/patch_1.2.sql
	 * @return return array and on 0 index is platform(mysql) and on 1 is patchName(patch_1.2.sql)
	 */
	@NonNull
	String[] getPlatformAndNameFromResourcePath(@NonNull String resourcePath);

}
