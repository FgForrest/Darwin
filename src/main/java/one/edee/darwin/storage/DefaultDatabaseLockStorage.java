package one.edee.darwin.storage;

import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.model.LockState;
import org.springframework.dao.EmptyResultDataAccessException;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;

/**
 * Default database implementation for {@link one.edee.darwin.locker.Locker}.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@CommonsLog
public class DefaultDatabaseLockStorage extends TransactionalDatabaseLockStorage {

	@Override
    protected LockState getDbProcessLock(final String processName, final LocalDateTime currentDate) {
		final String checkScript = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/lock_check.sql");
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
		final String insertScript = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/lock_insert.sql");
		jdbcTemplate.update(insertScript, processName, java.sql.Timestamp.valueOf(until), unlockKey);
		return LockState.LEASED;
	}

	@Override
    protected LockState releaseDbProcess(final String processName, final String unlockKey) throws IllegalStateException {
		final String deleteScript = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/lock_delete.sql");
		if (jdbcTemplate.update(deleteScript, processName, unlockKey) == 0) {
			final String msg = "No lock for process" + processName + " and unlockKey " + unlockKey + " was found!";
			log.error(msg);
			throw new IllegalStateException(msg);
		}
		return LockState.AVAILABLE;
	}

	@Override
    protected LockState renewDbLease(final LocalDateTime until, final String processName, final String unlockKey) {
		final String updateScript = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/lock_update.sql");
		if (jdbcTemplate.update(updateScript, java.sql.Timestamp.valueOf(until), processName, unlockKey) > 0) {
			return LockState.LEASED;
		} else {
			return LockState.AVAILABLE;
		}
	}
}
