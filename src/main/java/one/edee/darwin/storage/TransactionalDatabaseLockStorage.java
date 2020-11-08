package one.edee.darwin.storage;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.model.LockState;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Transaction database implementation.<br/>
 * In this implementation is used TransactionTemplate which wraps DB calls so it could be defined, whether Lock persister runs
 * in separate transaction or not.<br/>
 * If TransactionTemplate is not provided, direct calls are made (e.g. TransactionManager doesn't exist and so
 * TransactionTemplate simply cannot be created).
 *
 * @author Martin Veska, FG Forrest a.s. (c) 2012
 */
@EqualsAndHashCode(callSuper = true)
@Data
@CommonsLog
public abstract class TransactionalDatabaseLockStorage extends AbstractDatabaseStorage implements LockStorage {
    private TransactionTemplate transactionTemplate;

    @Override
    public LocalDateTime getCurrentDatabaseTime() {
        String dateScript = dbResourceAccessor.getTextContentFromResource(getPlatform().getFolderName() + "/lock_current_time.sql");
        final Instant databaseNow = jdbcTemplate.queryForObject(dateScript, Instant.class);
        return LocalDateTime.ofInstant(databaseNow, ZoneId.systemDefault());
    }

    @Override
    public LockState getProcessLock(final String processName, final LocalDateTime currentDate) {
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> getDbProcessLock(processName, currentDate));
        } else {
            return getDbProcessLock(processName, currentDate);
        }
    }

    @Override
    public LockState createLock(final String processName, final LocalDateTime until, final String unlockKey) {
        if (log.isDebugEnabled()) {
            log.debug("Creating lock for: " + processName + " with unlock key " + unlockKey);
        }
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> createDbLock(processName, until, unlockKey));
        } else {
            return createDbLock(processName, until, unlockKey);
        }
    }

    @Override
    public LockState releaseProcess(final String processName, final String unlockKey) {
        if (log.isDebugEnabled()) {
            log.debug("Removing lock for: " + processName + " with unlock key " + unlockKey + " (or expired).");
        }
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> releaseDbProcess(processName, unlockKey));
        } else {
            return releaseDbProcess(processName, unlockKey);
        }
    }

    @Override
    public LockState renewLease(final String processName, final String unlockKey, final LocalDateTime until) {
        if (log.isDebugEnabled()) {
            log.debug("Renewing lock for: " + processName + " with unlock key " +
                    unlockKey + " until " + until.format(DateTimeFormatter.BASIC_ISO_DATE));
        }
        if (transactionTemplate != null) {
            return transactionTemplate.execute(status -> renewDbLease(until, processName, unlockKey));
        } else {
            return renewDbLease(until, processName, unlockKey);
        }
    }

    protected abstract LockState getDbProcessLock(final String processName, final LocalDateTime currentDate);

    protected abstract LockState createDbLock(final String processName, final LocalDateTime until, final String unlockKey);

    protected abstract LockState releaseDbProcess(final String processName, final String unlockKey);

    protected abstract LockState renewDbLease(final LocalDateTime until, final String processName, final String unlockKey);
}
