package one.edee.darwin.storage;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Transaction database implementation.<br/>
 * In this implementation is used TransactionTemplate which wraps DB calls so it could be defined, whether Lock persister runs
 * in separate transaction or not.<br/>
 * If TransactionTemplate is not provided, direct calls are made (e.g. TransactionManager doesn't exist and so
 * TransactionTemplate simply cannot be created).
 *
 * @author Martin Veska, FG Forrest a.s. (c) 2012
 * @version $Id$
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class TransactionalDatabaseLockPersister extends AbstractDatabaseStorage implements LockPersister {
    private static final Log log = LogFactory.getLog(TransactionalDatabaseLockPersister.class);

    private TransactionTemplate transactionTemplate;

    @Override
    public Date getCurrentDatabaseTime() {
        String dateScript = dbResourceAccessor.getTextContentFromResource(getPlatform() + "/lock_current_time.sql");
        return jdbcTemplate.queryForObject(dateScript, Date.class);
    }

    @Override
    public int getProcessLock(final String processName, final Date currentDate) {
        if (transactionTemplate != null) {
            return (Integer) transactionTemplate.execute(new TransactionCallback() {
                                                             @Override
                                                             public Object doInTransaction(TransactionStatus status) {
                                                                 return getDbProcessLock(processName, currentDate);
                                                             }
                                                         }
            );
        } else {
            return (Integer) getDbProcessLock(processName, currentDate);
        }
    }

    @Override
    public void createLock(final String processName, final Date until, final String unlockKey) {
        if (log.isDebugEnabled()) {
            log.debug("Creating lock for: " + processName + " with unlock key " + unlockKey);
        }
        if (transactionTemplate != null) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                                            @Override
                                            protected void doInTransactionWithoutResult(TransactionStatus status) {
                                                createDbLock(processName, until, unlockKey);
                                            }
                                        }
            );
        } else {
            createDbLock(processName, until, unlockKey);
        }
    }

    @Override
    public int releaseProcess(final String processName, final String unlockKey) {
        if (log.isDebugEnabled()) {
            log.debug("Removing lock for: " + processName + " with unlock key " + unlockKey + " (or expired).");
        }
        if (transactionTemplate != null) {
            return (Integer) transactionTemplate.execute(new TransactionCallback() {
                                                             @Override
                                                             public Object doInTransaction(TransactionStatus status) {
                                                                 return releaseDbProcess(processName, unlockKey);
                                                             }
                                                         }
            );
        } else {
            return (Integer) releaseDbProcess(processName, unlockKey);
        }
    }

    @Override
    public int renewLease(final String processName, final String unlockKey, final Date until) {
        if (log.isDebugEnabled()) {
            SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
            log.debug("Renewing lock for: " + processName + " with unlock key " +
                    unlockKey + " until " + fmt.format(until));
        }
        if (transactionTemplate != null) {
            return (Integer) transactionTemplate.execute(new TransactionCallback() {
                                                             @Override
                                                             public Object doInTransaction(TransactionStatus status) {
                                                                 return renewDbLease(until, processName, unlockKey);
                                                             }
                                                         }
            );
        } else {
            return (Integer) renewDbLease(until, processName, unlockKey);
        }
    }

    protected abstract Object getDbProcessLock(final String processName, final Date currentDate);

    protected abstract void createDbLock(final String processName, final Date until, final String unlockKey);

    protected abstract Object releaseDbProcess(final String processName, final String unlockKey);

    protected abstract Object renewDbLease(final Date until, final String processName, final String unlockKey);
}
