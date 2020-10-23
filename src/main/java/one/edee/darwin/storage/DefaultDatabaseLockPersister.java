package one.edee.darwin.storage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.EmptyResultDataAccessException;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Default database implementation.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 * @version $Id$
 */
public class DefaultDatabaseLockPersister extends TransactionalDatabaseLockPersister {
	private static final Log log = LogFactory.getLog(DefaultDatabaseLockPersister.class);

	@Override
    protected Object getDbProcessLock(final String processName, final Date currentDate) {
		String checkScript = dbResourceAccessor.getTextContentFromResource(getPlatform() + "/lock_check.sql");
		try {
			Date leaseUntil = jdbcTemplate.queryForObject(checkScript, new Object[]{processName}, Date.class);
			if(leaseUntil != null) {
				if(leaseUntil.getTime() > currentDate.getTime()) {
					if(log.isDebugEnabled()) {
						SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                        log.debug("Found unexpired lock on process " + processName +
                                ". Lock will expire automatically on " + fmt.format(leaseUntil));
                    }
                    return 1;
                } else {
					if(log.isDebugEnabled()) {
						log.debug("Found expired lock on process " + processName + ". Lock could be broken.");
					}
					return 2;
				}
			}
			return 0;
		}
		catch(EmptyResultDataAccessException ignored) {
			//no lock found
			return 0;
		}
	}

	@Override
    protected void createDbLock(final String processName, final Date until, final String unlockKey) {
		String insertScript = dbResourceAccessor.getTextContentFromResource(getPlatform() + "/lock_insert.sql");
		jdbcTemplate.update(insertScript, processName, until, unlockKey);
	}

	@Override
    protected Object releaseDbProcess(final String processName, final String unlockKey) {
		String deleteScript = dbResourceAccessor.getTextContentFromResource(getPlatform() + "/lock_delete.sql");
		return jdbcTemplate.update(deleteScript, processName, unlockKey);
	}

	@Override
    protected Object renewDbLease(final Date until, final String processName, final String unlockKey) {
		String updateScript = dbResourceAccessor.getTextContentFromResource(getPlatform() + "/lock_update.sql");
		return jdbcTemplate.update(updateScript, until, processName, unlockKey);
	}
}
