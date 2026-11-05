package one.edee.darwin.storage;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lock statement timeout must be opt-in: on H2 a statement timeout is a session setting that would stay on pooled
 * connections and cancel unrelated long statements (e.g. migrations) executed later on the same connection.
 */
class DefaultDatabaseLockStorageTimeoutTest {

    @Test
    void noStatementTimeoutByDefault() {
        DefaultDatabaseLockStorage storage = new DefaultDatabaseLockStorage();
        storage.setDataSource(new SingleConnectionDataSource("jdbc:h2:mem:lock-timeout-default", "sa", "", true));
        // -1 = JdbcTemplate default, Statement#setQueryTimeout is never called (0 would still reset an H2 session)
        assertEquals(-1, storage.jdbcTemplate.getQueryTimeout());
    }

    @Test
    void configuredStatementTimeoutIsAppliedRegardlessOfOrder() {
        DefaultDatabaseLockStorage before = new DefaultDatabaseLockStorage();
        before.setQueryTimeoutSeconds(5);
        before.setDataSource(new SingleConnectionDataSource("jdbc:h2:mem:lock-timeout-before", "sa", "", true));
        assertEquals(5, before.jdbcTemplate.getQueryTimeout());

        DefaultDatabaseLockStorage after = new DefaultDatabaseLockStorage();
        after.setDataSource(new SingleConnectionDataSource("jdbc:h2:mem:lock-timeout-after", "sa", "", true));
        after.setQueryTimeoutSeconds(5);
        assertEquals(5, after.jdbcTemplate.getQueryTimeout());
        after.setQueryTimeoutSeconds(0);
        assertEquals(-1, after.jdbcTemplate.getQueryTimeout());
    }
}
