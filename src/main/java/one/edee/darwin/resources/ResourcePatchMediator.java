package one.edee.darwin.resources;

import one.edee.darwin.model.InitiatingPatch;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.Platform;
import one.edee.darwin.storage.DarwinStorage;
import one.edee.darwin.storage.StorageChecker;
import org.springframework.core.io.Resource;

import java.time.LocalDateTime;

/**
 * This method provide patches, when is patch and sql table active, then write information on them and take ID. But when
 * is Db inactive then creates patch with implicit ID(0)
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
	                          Platform platform, DarwinStorage darwinStorage,
	                          StorageChecker storageChecker,
	                          PatchType patchType) {
        final Patch[] patches = new Patch[resources.length];
        if (storageChecker.existPatchAndSqlTable()) {
            final boolean anyPatchRecordedFor = darwinStorage.isAnyPatchRecordedFor(componentName);
            for (int i = 0; i < resources.length; i++) {
				final Resource resource = resources[i];
				final String patchName = resourceNameAnalyzer.getPatchNameFromResource(resource);
                boolean dbPatch = anyPatchRecordedFor && patchType == PatchType.EVOLVE && resourceMatcher.isResourceAcceptable(PatchType.EVOLVE, patchName);
                patches[i] = dbPatch ?
						convertToPatchPersistedInDatabase(resource, componentName, platform, darwinStorage) :
						convertToVirtualPatch(resource, componentName, platform);
            }
        } else {
            for (int i = 0; i < resources.length; i++) {
                patches[i] = convertToVirtualPatch(resources[i], componentName, platform);
            }
        }
        return patches;
    }

    private Patch convertToVirtualPatch(Resource resource, String componentName, Platform platform) {
        return new Patch(
                resourceNameAnalyzer.getPatchNameFromResource(resource),
                componentName,
                platform,
				LocalDateTime.now()
        );
    }

    private Patch convertToPatchPersistedInDatabase(Resource o, String componentName,
													Platform platform, DarwinStorage darwinStorage) {
        return new InitiatingPatch(
                resourceNameAnalyzer.getPatchNameFromResource(o),
                componentName,
		        LocalDateTime.now(),
                platform,
		        darwinStorage
        );
    }
}
