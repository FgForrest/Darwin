package one.edee.darwin.resources;

import lombok.NonNull;
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

    public ResourcePatchMediator(@NonNull ResourceMatcher resourceMatcher) {
        this.resourceMatcher = resourceMatcher;
    }

    /**
     * Retrieves an array of {@link Patch} objects based on the provided resources and the given context.
     * This method determines if a patch needs to be persisted in the database or created virtually
     * based on the storage and patch type provided.
     *
     * @param resources      an array of {@link Resource} instances representing the resources to be converted into patches, must not be null
     * @param componentName  the name of the component associated with the patches, must not be null
     * @param platform       the platform associated with the patches, must not be null
     * @param darwinStorage  the storage system to be consulted for persistence details, must not be null
     * @param storageChecker the storage checker to verify if patches and SQL table exist, must not be null
     * @param patchType      the type of patch to be created, must not be null
     **/
    @NonNull
    public Patch[] getPatches(
        @NonNull Resource[] resources,
        @NonNull String componentName,
        @NonNull Platform platform,
        @NonNull DarwinStorage darwinStorage,
        @NonNull StorageChecker storageChecker,
        @NonNull PatchType patchType
    ) {
        final Patch[] patches = new Patch[resources.length];
        if (storageChecker.existPatchAndSqlTable()) {
            final boolean anyPatchRecordedFor = darwinStorage.isAnyPatchRecordedFor(componentName);
            for (int i = 0; i < resources.length; i++) {
                final Resource resource = resources[i];
                final String patchName = resourceMatcher.getPatchNameFromResource(resource);
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

    /**
     * Converts the provided resource to a virtual patch.
     * This method creates a new {@link Patch} instance using the resource, component name,
     * and platform, and assigns the current timestamp to indicate when the patch was created.
     *
     * @param resource      the resource to be converted into a patch, must not be null
     * @param componentName the name of the component associated with the resource, must not be null
     * @param platform      the platform related to the patch, must not be null
     * @return a new instance of {@link Patch} containing the converted data
     */
    @NonNull
    private Patch convertToVirtualPatch(
        @NonNull Resource resource,
        @NonNull String componentName,
        @NonNull Platform platform
    ) {
        return new Patch(
            resourceMatcher.getPatchNameFromResource(resource),
            componentName,
            platform,
            LocalDateTime.now()
        );
    }

    /**
     * Converts the given resource into a persistable database patch.
     * This method creates a new {@link InitiatingPatch} object using the provided inputs
     * and assigns the current timestamp for when the patch is created.
     *
     * @param resource      the resource to be converted to a patch
     * @param componentName the name of the component associated with the resource
     * @param platform      the platform for which the patch is applicable
     * @param darwinStorage the storage system to associate the patch with
     * @return a {@link Patch} object persisted with necessary details for the database
     */
    @NonNull
    private Patch convertToPatchPersistedInDatabase(
        @NonNull Resource resource,
        @NonNull String componentName,
        @NonNull Platform platform,
        @NonNull DarwinStorage darwinStorage
    ) {
        return new InitiatingPatch(
            resourceMatcher.getPatchNameFromResource(resource),
            componentName,
            LocalDateTime.now(),
            platform,
            darwinStorage
        );
    }
}
