package one.edee.darwin.storage;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.model.Patch;
import one.edee.darwin.model.Platform;
import one.edee.darwin.model.version.VersionDescriptor;
import one.edee.darwin.resources.PatchType;
import one.edee.darwin.resources.ResourceMatcher;
import one.edee.darwin.resources.ResourcePatchMediator;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of storage checker just runs selected scripts and when it ends without any exception
 * it assumes, that version of the storage is that with last executed script.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@RequiredArgsConstructor
@CommonsLog
public class DefaultDatabaseStorageChecker extends AbstractDatabaseStorage implements StorageChecker {
    private final ResourcePatchMediator resourcePatchMediator;
    @Setter @Getter private ResourceMatcher resourceMatcher;
	@Setter @Getter private boolean patchAndTableExists;

	@Nullable
	@Override
    public VersionDescriptor guessVersion(
		@NonNull String componentName,
		@NonNull DarwinStorage darwinStorage
	) {
        final Platform platform = getPlatform();
        final Resource[] sortedResourceList = resourceAccessor.getSortedResourceList(platform);
        final Patch[] patches = resourcePatchMediator.getPatches(
        		sortedResourceList, componentName, platform, darwinStorage, this,
				PatchType.GUESS);
        VersionDescriptor guessedVersion = null;

        for (Patch patch : patches) {
            if (resourceMatcher.isResourceAcceptable(PatchType.GUESS, patch.getPatchName())) {
                VersionDescriptor resourceVersion = resourceMatcher.getVersionFromPatch(patch);
				Assert.isTrue(resourceVersion != null, "Resource version must not be null for patch: " + patch.getPatchName());

                long start = System.currentTimeMillis();
                boolean result = executeScript(patch);
                long stop = System.currentTimeMillis();

                if (result) {
                    guessedVersion = resourceVersion;
	                markGuessedPatchAsFinished(darwinStorage, patch, resourceVersion, start, stop);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Storage not compatible with version: " + resourceVersion);
                    }
                    break;
                }
            }
        }
        return guessedVersion;
    }

	@Override
	public boolean guessPatchAlreadyApplied(
		@NonNull String componentName,
		@NonNull DarwinStorage darwinStorage,
		@NonNull VersionDescriptor checkedVersion
	) {
		final Platform platform = getPlatform();
		final Resource[] sortedResourceList = resourceAccessor.getSortedResourceList(platform);
		final Patch[] patches = resourcePatchMediator.getPatches(
				sortedResourceList, componentName, platform, darwinStorage, this,
				PatchType.GUESS);

		for (Patch patch : patches) {
			if (resourceMatcher.isResourceAcceptable(PatchType.GUESS, patch.getPatchName())) {
				VersionDescriptor patchVersion = resourceMatcher.getVersionFromPatch(patch);
				if (patchVersion != null && Objects.equals(patchVersion, checkedVersion)) {
					long start = System.currentTimeMillis();
					boolean result = executeScript(patch);
					long stop = System.currentTimeMillis();
					if (result) {
						markGuessedPatchAsFinished(darwinStorage, patch, patchVersion, start, stop);
						return true;
					}
				}
			}
		}
		return false;
	}

	private void markGuessedPatchAsFinished(
		@NonNull DarwinStorage darwinStorage,
		@NonNull Patch patch,
		@NonNull VersionDescriptor resourceVersion,
		long start,
		long stop
	) {
		if (log.isDebugEnabled()) {
			log.debug("Storage compatible with version: " + resourceVersion);
		}
		if (existPatchAndSqlTable()) {
			long duration = stop - start;
			patch.setProcessTime((int) duration);
			patch.setFinishedOn(LocalDateTime.now());
			darwinStorage.markPatchAsFinished(
					darwinStorage.insertPatchToDatabase(
							patch.getPatchName(), patch.getComponentName(),
							LocalDateTime.now(), getPlatform()
					)
			);
		}
	}

	@Override
    public boolean existPatchAndSqlTable() {
        if (patchAndTableExists) {
            //due to performance optimization, we assume that once tables exists they continue to exists once for all
            return true;
        } else {
			return existsPatchAndSqlTableNoCache();
        }
    }

	@SuppressWarnings("SqlSourceToSinkFlow")
    public boolean existsPatchAndSqlTableNoCache() {
		final String sqlForPatch = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/check_patchTableExist.sql");
		final String sqlForSQL = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/check_sqlCommandTableExists.sql");
		try {
			jdbcTemplate.execute(sqlForPatch);
			jdbcTemplate.execute(sqlForSQL);
			patchAndTableExists = true;
			return true;
		} catch (BadSqlGrammarException ignored) {
			//if any of the SQL files fails - target table is not present in database
			return false;
		}
	}

	@SuppressWarnings("SqlSourceToSinkFlow")
    private boolean executeScript(@NonNull Patch patch) {
		final List<String> tokenizedScript = resourceAccessor.getTokenizedSQLScriptContentFromResource(patch.getResourcesPath());
		try {
			for (String sql : tokenizedScript) {
				if (sql.trim().toLowerCase().matches("select\\s*count\\(.*")) {
					final Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
					if (result == null || result == 0) {
						return false;
					}
				} else {
					jdbcTemplate.execute(sql);
				}
			}
			return true;
		} catch (BadSqlGrammarException ex) {
			//guess script failed
			return false;
        } catch (DataAccessException ex) {
            String msg = "Failed to execute script: " + resourceAccessor.getTextContentFromResource(patch.getResourcesPath());
            log.error(msg, ex);
            return false;

        }
    }

}
