package one.edee.darwin.storage;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.model.LockState;
import one.edee.darwin.model.Platform;
import org.springframework.dao.EmptyResultDataAccessException;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.sql.Types;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default database implementation for {@link one.edee.darwin.locker.Locker}.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@CommonsLog
public class DefaultDatabaseLockStorage extends TransactionalDatabaseLockStorage {
	private static final Map<LockStorageStatementWithPlatform, String> STATEMENTS_CACHE = new ConcurrentHashMap<>();

	@RequiredArgsConstructor
	private enum LockStorageStatement {
		LOCK_CHECK("lock_check.sql"),
		LOCK_CURRENT_TIME("lock_current_time.sql"),
		LOCK_DELETE("lock_delete.sql"),
		LOCK_INSTANCE_DELETE("lock_instance_delete.sql"),
		LOCK_INSERT("lock_insert.sql"),
		LOCK_UPDATE("lock_update.sql");

		@Getter private final String fileName;
	}

	@Data
	private static class LockStorageStatementWithPlatform {
		private final LockStorageStatement statementType;
		private final Platform platform;
	}

    /**
     * Optional statement timeout of lock operations in seconds so that a blocked statement cannot hold a renewal
     * thread indefinitely. Zero (default) or negative value leaves statements untouched (JDBC driver default).
     * Does not limit connection acquisition.
     */
    private int queryTimeoutSeconds;

    @Override
    public void setDataSource(DataSource source) {
        super.setDataSource(source);
        applyQueryTimeout();
    }

    /**
     * Sets statement timeout of lock operations in seconds; zero or negative value leaves statements untouched.
     * Do not use with H2 on a shared or pooled data source: H2 applies the timeout to the whole session, so it stays
     * on the connection and cancels unrelated longer statements executed on it later.
     */
    public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
        applyQueryTimeout();
    }

    private void applyQueryTimeout() {
        if (jdbcTemplate != null) {
            // -1 is the JdbcTemplate default: Statement#setQueryTimeout is not called at all
            jdbcTemplate.setQueryTimeout(queryTimeoutSeconds > 0 ? queryTimeoutSeconds : -1);
        }
    }

	@Override
	protected int releaseDbLocksForInstance(final String instanceId) {
		final String instanceDeleteScript = STATEMENTS_CACHE.computeIfAbsent(
				getKey(LockStorageStatement.LOCK_INSTANCE_DELETE), this::readContentFromResource
		);
		try {
			return jdbcTemplate.update(instanceDeleteScript, "%"+instanceId);
		} catch (EmptyResultDataAccessException ignored) {
			//no lock found
			return 0;
		}
	}

	@Override
    protected LockState getDbProcessLock(final String processName, final LocalDateTime currentDate) {
		final String checkScript = STATEMENTS_CACHE.computeIfAbsent(
				getKey(LockStorageStatement.LOCK_CHECK), this::readContentFromResource
		);
		try {
			final LocalDateTime leaseUntil = jdbcTemplate.queryForObject(checkScript, new Object[]{processName}, LocalDateTime.class);
			if(leaseUntil != null) {
				if(leaseUntil.isAfter(currentDate)) {
					if(log.isDebugEnabled()) {
                        log.debug("Found unexpired lock on process " + processName +
                                ". Lock will expire automatically on " + leaseUntil);
                    }
                    return LockState.LEASED;
                } else {
					if(log.isDebugEnabled()) {
						log.debug("Found expired lock on process " + processName + ". Lock could be broken.");
					}
					return LockState.LEASED_EXPIRED;
				}
			}
			return LockState.AVAILABLE;
		}
		catch(EmptyResultDataAccessException ignored) {
			//no lock found
			return LockState.AVAILABLE;
		}
	}

	@Override
    protected LockState createDbLock(final String processName, final LocalDateTime until, final String unlockKey) {
		final String insertScript = STATEMENTS_CACHE.computeIfAbsent(
				getKey(LockStorageStatement.LOCK_INSERT), this::readContentFromResource
		);
		jdbcTemplate.update(insertScript, processName, java.sql.Timestamp.valueOf(until), unlockKey);
		return LockState.LEASED;
	}

	@Override
    protected LockState releaseDbProcess(final String processName, final String unlockKey) throws IllegalStateException {
		final String deleteScript = STATEMENTS_CACHE.computeIfAbsent(
				getKey(LockStorageStatement.LOCK_DELETE), this::readContentFromResource
		);
		if (jdbcTemplate.update(deleteScript, new Object[]{processName, unlockKey, unlockKey},
            new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR}) == 0 && unlockKey != null) {
			final String msg = "No owned lock for process " + processName + " was found.";
			log.error(msg);
			throw new IllegalStateException(msg);
		}
		return LockState.AVAILABLE;
	}

	@Override
    protected LockState renewDbLease(final LocalDateTime until, final String processName, final String unlockKey) {
		final String updateScript = STATEMENTS_CACHE.computeIfAbsent(
				getKey(LockStorageStatement.LOCK_UPDATE), this::readContentFromResource
		);
		if (jdbcTemplate.update(updateScript, java.sql.Timestamp.valueOf(until), processName, unlockKey) > 0) {
			return LockState.LEASED;
		} else {
			return LockState.AVAILABLE;
		}
	}

	private LockStorageStatementWithPlatform getKey(LockStorageStatement statementType) {
		return new LockStorageStatementWithPlatform(statementType, getPlatform());
	}

	private String readContentFromResource(LockStorageStatementWithPlatform statementTypeWithPlatform) {
		return dbResourceAccessor.getTextContentFromResource(
				statementTypeWithPlatform.getPlatform().getFolderName() + "/" +
						statementTypeWithPlatform.getStatementType().getFileName()
		);
	}


}
