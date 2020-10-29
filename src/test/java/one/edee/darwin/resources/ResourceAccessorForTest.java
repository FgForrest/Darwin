package one.edee.darwin.resources;

import one.edee.darwin.model.Platform;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * This implementation can load patches from two separate directories ;)
 *
 * @author Radek Salay, FG Forest a.s. 7/15/16.
 */
public class ResourceAccessorForTest extends DefaultResourceAccessor {
	private String resourcePathForPatch;

    public void setResourcePathForPatch(String resourcePathForPatch) {
        this.resourcePathForPatch = resourcePathForPatch;
    }

    @Override
    public Resource[] getSortedResourceList(Platform platform) {
        Resource[] baseResources = super.getSortedResourceList(platform);
        Resource[] resources = null;

        if (resourcePathForPatch!=null) {
            String normalizedPath = normalizePath(resourcePathForPatch, platform.getFolderName(), true);
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
			resources = getResources(normalizedPath, resolver);
        }

        Collection<Resource> baseResourcesAsList = baseResources != null ? Arrays.asList(baseResources) : Collections.emptyList();
        Collection<Resource> patchResourcesAsList = resources != null ? Arrays.asList(resources) : Collections.emptyList();
		Collection<Resource> allResource = new ArrayList<Resource>(baseResourcesAsList.size() + patchResourcesAsList.size());
        allResource.addAll(baseResourcesAsList);
        allResource.addAll(patchResourcesAsList);

        return allResource.toArray(new Resource[0]);

    }
    @Override
    public String getTextContentFromResource(String resourcePath) {
        try {
            String s =super.getTextContentFromResource(resourcePath);
            if (s !=null) {
                return s;
            }
            throw new RuntimeException();
        } catch (Exception e) {
            String normalizedPath = normalizePath(resourcePathForPatch, resourcePath, false);
            //base path may contain fe: classpath*:/directory ... so when looking up for specific resource, asterisk must be removed
            normalizedPath = normalizedPath.replaceAll("\\*", "");
            Resource resource = resourceLoader.getResource(normalizedPath);

            return readResource(resourcePath, normalizedPath, resource);
        }
    }
}
