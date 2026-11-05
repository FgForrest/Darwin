package one.edee.darwin.locker.h2;

import one.edee.darwin.exception.ProcessIsLockedException;
import one.edee.darwin.locker.Locker;
import one.edee.darwin.locker.LockerTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Radek Salay, FG Forest a.s. 7/14/16.
 */
@ActiveProfiles(value = "H2")
@Profile(value = "H2")
public class LockerTestH2 extends LockerTest {
    private final Locker locker;
    private final PlatformTransactionManager transactionManager;

    @Autowired
    public LockerTestH2(Locker locker, PlatformTransactionManager transactionManager) {
        this.locker = locker;
        this.transactionManager = transactionManager;
    }

    @Test
    void renewalUsesCurrentDatabaseTimeInsideAnExistingTransaction() throws Exception {
        String process = "transaction-clock";
        String key = locker.leaseProcess(process, LocalDateTime.now().plusSeconds(30));
        try {
            new TransactionTemplate(transactionManager).execute(status -> {
                // H2 freezes CURRENT_TIMESTAMP at this transaction's first statement.
                getJdbcTemplate().queryForObject("SELECT CURRENT_TIMESTAMP", Object.class);
                try {
                    Thread.sleep(1500);
                    locker.renewLease(process, key, LocalDateTime.now().plusSeconds(1));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                } catch (ProcessIsLockedException e) {
                    throw new IllegalStateException(e);
                }
                return null;
            });
            assertFalse(locker.canLease(process), "Renewal must not write a deadline based on stale transaction time");
        } finally {
            locker.releaseProcess(process, key);
        }
    }
}
