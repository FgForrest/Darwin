package one.edee.darwin.storage;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.model.LockState;
import one.edee.darwin.model.Platform;
import org.springframework.dao.EmptyResultDataAccessException;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
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
		LOCK_INSERT("lock_insert.sql"),
		LOCK_UPDATE("lock_update.sql");

		@Getter private final String fileName;
	}

	@Data
	private static class LockStorageStatementWithPlatform {
		private final LockStorageStatement statementType;
		private final Platform platform;
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
						SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                        log.debug("Found unexpired lock on process " + processName +
                                ". Lock will expire automatically on " + fmt.format(leaseUntil));
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
		if (jdbcTemplate.update(deleteScript, processName, unlockKey) == 0) {
			final String msg = "No lock for process" + processName + " and unlockKey " + unlockKey + " was found!";
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
