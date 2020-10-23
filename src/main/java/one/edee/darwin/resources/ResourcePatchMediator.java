package one.edee.darwin.resources;

import one.edee.darwin.model.InitiatingPatch;
import one.edee.darwin.model.Patch;
import one.edee.darwin.storage.AutoUpdatePersister;
import one.edee.darwin.storage.StorageChecker;
import org.springframework.core.io.Resource;

import java.util.Date;

/**
 * This method provide patches, when is patch and sql table active, then write information on them and take ID. But when is Db inactive then create pach with implicit ID(0)
 *
 * @author Radek Salay, FG Forest a.s. 6/27/16.
 */
public class ResourcePatchMediator {
    private final ResourceMatcher resourceMatcher;
    private final ResourceNameAnalyzer resourceNameAnalyzer;

	public ResourcePatchMediator(ResourceMatcher resourceMatcher, ResourceNameAnalyzer resourceNameAnalyzer) {
		this.resourceMatcher = resourceMatcher;
		this.resourceNameAnalyzer = resourceNameAnalyzer;
	}

	public Patch[] getPatches(Resource[] resources, String componentName,
							  String platform, AutoUpdatePersister autoUpdatePersister,
							  StorageChecker storageChecker,
							  PatchMode patchMode) {
        Patch[] patches = new Patch[resources.length];
        if (storageChecker.existPatchAndSqlTable()) {
            final boolean anyPatchRecordedFor = autoUpdatePersister.isAnyPatchRecordedFor(componentName);
            for (int i = 0; i < resources.length; i++) {
				final Resource resource = resources[i];
				final String patchName = resourceNameAnalyzer.getPatchNameFromResource(resource);
                boolean dbPatch = anyPatchRecordedFor && patchMode == PatchMode.Patch && resourceMatcher.isResourceAcceptable(PatchMode.Patch, patchName);
                patches[i] = dbPatch ?
						convertToPatchPersistedInDatabase(resource, componentName, platform, autoUpdatePersister) :
						convertToVirtualPatch(resource, componentName, platform);
            }
        } else {
            for (int i = 0; i < resources.length; i++) {
                patches[i] = convertToVirtualPatch(resources[i], componentName, platform);
            }
        }
        return patches;
    }

    private Patch convertToVirtualPatch(Resource resource, String componentName, String platform) {
        return new Patch(
                resourceNameAnalyzer.getPatchNameFromResource(resource),
                componentName,
                platform,
				new Date()
        );
    }

    private Patch convertToPatchPersistedInDatabase(Resource o, String componentName,
													String platform, AutoUpdatePersister autoUpdatePersister) {
        return new InitiatingPatch(
                resourceNameAnalyzer.getPatchNameFromResource(o),
                componentName,
                new Date(),
                platform,
                autoUpdatePersister
        );
    }
}
