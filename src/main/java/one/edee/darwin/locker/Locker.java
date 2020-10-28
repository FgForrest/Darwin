package one.edee.darwin.locker;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.exception.ProcessIsLockedException;
import one.edee.darwin.locker.internal.CheckLockTimerTask;
import one.edee.darwin.model.LockState;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.storage.DefaultDatabaseLockStorage;
import one.edee.darwin.storage.LockStorage;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * # Process synchronization in a cluster using shared RDBMS
 *
 * The Locker class has been created for the needs of Darwin, but you can use it in your application as well. Locker will
 * ensure that your processes that are not written for concurrent execution are executed one at a time eve in cluster
 * environment with multiple JVMs. The single prerequisite is a shared relational database that is accessible from all the
 * nodes.
 *
 * Locker provides the following main methods:
 *
 * - `String leaseProcess (String processName, Date until) throws ProcessIsLockedException`
 * - `void renewLease (String processName, String unlockKey, Date until) throws ProcessIsLockedException`
 * - `void releaseProcess (String processName, String unlockKey)`
 *
 * The first method will get the lock for your process, the second method will release the lock. Locker will not allow
 * acquiring two locks to the process with the same name. However, the whole principle is based on the form of leasing -
 * the lock is only borrowed for the specified period (`until` argument). This leasing mechanism addresses the problem
 * of unexpected application termination when the locks are not released at all. In such situation processes can recover
 * automatically after the application restart one the lock lease period expires.
 *
 * It's recommended to specify the `until` argument to a moment that to process is certainly finished. The better yet
 * to add a considerable reserve time so that the process won't exceed leased time. When process contains an inner loop for
 * paged records processing, it's recommended to prolong lease by calling `renewLease` method after each page has been
 * processed.
 *
 * At the end the process is expected to return acquired lock by passing random key acquired during lease process. For
 * returning the key use `releaseProcess` method.
 *
 * ## Recommended usage
 *
 * See following example for recommended usage:
 *
 * ``` java
 * String theKey = null;
 * try {
 *     theKey = locker.leaseProcess("myProcess", LocalDateTime.now().plusMinutes(30));
 *     // do your stuff
 *     while (pageOfRecords.hasNextPage()) {
 *         // process page
 *         locker.renewLease("myProcess", theKey, LocalDateTime.now().plusMinutes(30));
 *     }
 * } catch (ProcessIsLockedException ex) {
 *     // process is running somewhere else - just log it
 *     logger.info("Process myProcess is running elsewhere, skipping this execution and will try next time.");
 * } finally {
 *     Optional.ofNullable(theKey)
 *             .ifPresent(it -> locker.releaseProcess("myProcess", it));
 * }
 * ```
 *
 * ## Automatic lock extension
 *
 * If you cannot estimate the proper lease time you can take advantage of automatic asynchronous lock prolonging process.
 * There are two special forms of lease methods that accept `LockRestorer` implementation:
 *
 * - `String leaseProcess(String processName, Date until, LockRestorer lockerRestorer)`
 * - `String leaseProcess(String processName, Date until, int waitForLockInMilliseconds, LockRestorer lockRestorer)`
 *
 * The last parameter represents your logic implementing the LockRestorer interface that returns flag signalizing whether
 * your process already finished or not.
 *
 * By calling these lease methods a new instance of CheckLockTimerTask is created and scheduled and periodically calls your
 * LockRestorer implementation to determine whether lock needs to be prolonged.
 *
 * Lock is renewed as long as:
 *
 * - the maximum number of lock extensions has been reached (the maximum number is defined by the constant MAX_RENEW_COUNT = 10)
 * - the lock has already been unlocked using the releaseProcess method
 * - the process signalizes TRUE in method lockRestorer.isFinished()
 *
 * CheckLockerTimerTask is triggered after 70% of the lock validity (eg if the lock validity is set to 10min, the
 * lock is extended after 7 minutes). There is no hard guarantee the CheckLockerTimerTask will be invoked by the system.
 * It uses standard `java.util.concurrent.ScheduledExecutorService.scheduleAtFixedRate` which may not invoke tasks when
 * system is under pressure.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@CommonsLog
public class Locker implements InitializingBean, ApplicationContextAware {
    private static final float CHECK_RENEW_RATIO = 0.7f;

	/**
	 * If set to true Locker with silently disable itself in case datasource is not present in application context.
	 */
    @Setter private boolean skipIfDataSourceNotPresent = true;
    /**
     * Name of the {@link DataSource} bean that will be looked up in {@link #applicationContext} in case no data source
     * is supplied from outside.
     */
    @Setter private String dataSourceName = "dataSource";
    /**
     * Name of the {@link PlatformTransactionManager} bean that will be looked up in {@link #applicationContext} in case
     * no transaction manager is supplied from outside.
     */
    @Setter private String transactionManagerName = "transactionManager";

    @Setter private ApplicationContext applicationContext;
    @Setter private LockStorage lockStorage;
    @Setter private ResourceAccessor resourceAccessor;
    @Getter private final Map<String, LockRestorer> processMap = new ConcurrentHashMap<>();

    /**
     * Internal flag, if set to TRUE, locker will throw exceptions on every call.
     * Locker in this state is inoperable.
     */
    private boolean switchOff;

    /**
     * Creates lock persister on specific dataSource and transactionManager.
     *
     * @param ds data source
     * @param transactionManager transaction manager
     * @param resourceAccessor implementation for accessing SQL queries
     * @param resourceLoader implementation for loading Spring {@link org.springframework.core.io.Resource}
     * @return default {@link LockStorage} implementation
     */
    public static LockStorage createDefaultLockPersister(
            DataSource ds, PlatformTransactionManager transactionManager,
            ResourceAccessor resourceAccessor, ResourceLoader resourceLoader) {
        final DefaultDatabaseLockStorage lockPersister = new DefaultDatabaseLockStorage();
        lockPersister.setResourceAccessor(resourceAccessor);
        lockPersister.setDataSource(ds);
        lockPersister.setTransactionManager(transactionManager);
		lockPersister.setResourceLoader(resourceLoader);

        final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        lockPersister.setTransactionTemplate(transactionTemplate);

        return lockPersister;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(resourceAccessor, "Locker needs resourceAccessor property to be not null!");

        //defaults
        final ConfigurableListableBeanFactory beanFactory = ((AbstractApplicationContext) applicationContext).getBeanFactory();
        final boolean dataSourcePresent = beanFactory.containsBean(dataSourceName);
        final boolean transactionManagerPresent = beanFactory.containsBean(transactionManagerName);
        if (dataSourcePresent) {
            final DataSource ds = (DataSource) applicationContext.getBean(dataSourceName);
            final PlatformTransactionManager transactionManager = transactionManagerPresent ?
                    (PlatformTransactionManager) applicationContext.getBean(transactionManagerName) : null;
            if (lockStorage == null) {
                lockStorage = createDefaultLockPersister(ds, transactionManager, resourceAccessor, applicationContext);
            }
        } else {
            if (skipIfDataSourceNotPresent) {
                switchOff = true;
            } else {
                throw new IllegalStateException("DataSource not accessible and skipIfDataSourceNotPresent " +
                        "flag is not set. Cannot perform database locking.");
            }
        }
    }

    /**
     * Returns true if process can be leased. Result is not guaranteed though - method may return TRUE at one time
     * and event then leasing attempt in the next second may fail.
     */
    public boolean canLease(String processName) {
        return lockStorage.getProcessLock(processName, normalizeDate(LocalDateTime.now())) != LockState.LEASED;
    }

    /**
     * Method stores lock on particular process.
     *
     * @param processName name of the process we want to lock
     * @param until       date until lock should be kept providing no one has unlock it by then
     * @return key for unlocking stored lock
     */
    public String leaseProcess(final String processName, final LocalDateTime until, int waitForLockInMilliseconds)
            throws ProcessIsLockedException {
        return doWithRetry(() -> {
            try {
                return leaseProcess(processName, until);
            } catch (ProcessIsLockedException e) {
                // mask exception to RuntimeException
                throw new RuntimeException(e);
            }
        }, waitForLockInMilliseconds, 10);
    }

    /**
     * Method stores lock on particular process.
     *
     * @param processName name of the process we want to lock
     * @param until date until lock should be kept providing no one has unlock it by then
     * @return key for unlocking stored lock
     */
    public String leaseProcess(String processName, LocalDateTime until) throws ProcessIsLockedException {
        checkStatus();
        //verify there is no lock
        checkExistingLock(processName);

        try {
            until = normalizeDate(until);
            final String unlockKey = Long.toHexString(System.currentTimeMillis());
            final LockState lockState = lockStorage.createLock(processName, until, unlockKey);
            Assert.isTrue(lockState == LockState.LEASED);

            if (log.isDebugEnabled()) {
                SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                log.debug("Process " + processName + " locked with unlockKey " + unlockKey +
                        " until " + fmt.format(until) + "");
            }

            return unlockKey;
        } catch (DataIntegrityViolationException ex) {
            //create lock could happen simultaneously - in that case throw ProcessIsLockedException
            final String msg = "Process " + processName + " has a foreign valid lock. Cannot register new one!";
            log.warn(msg);
            throw new ProcessIsLockedException(msg);
        }

    }

    /**
     * Method stores lock on particular process and start lock restorer.
     *
     * @param processName name of the process we want to lock
     * @param until date until lock should be kept providing no one has unlock it by then
     * @throws ProcessIsLockedException when lock is already leased
     */
    public String leaseProcess(String processName, LocalDateTime until, LockRestorer lockerRestorer) throws ProcessIsLockedException {
        final String unlockKey = leaseProcess(processName, until);
        setupCheckLockTimerTask(processName, until, lockerRestorer, LocalDateTime.now(), unlockKey);
        return unlockKey;
    }

    /**
     * Method stores lock on particular process and start lock restorer.
     *
     * @param processName name of the process we want to lock
     * @param until date until lock should be kept providing no one has unlock it by then
     * @return key for unlocking stored lock
     * @throws ProcessIsLockedException when lock is already leased
     */
    public String leaseProcess(String processName, LocalDateTime until, int waitForLockInMilliseconds, LockRestorer lockerRestorer) throws ProcessIsLockedException {
        final String unlockKey = leaseProcess(processName, until, waitForLockInMilliseconds);
        setupCheckLockTimerTask(processName, until, lockerRestorer, LocalDateTime.now(), unlockKey);
        return unlockKey;
    }

    /**
     * Renews lease date for particular process, if you have correct unlock key (otherwise exeption is thrown)
     *
     * @param processName name of the process we want to have the lock renewed
     * @param unlockKey key obtained during lock leasing
     * @param until date until lock should be kept providing no one has unlock it by then
     * @throws ProcessIsLockedException when lock is already leased
     */
    public void renewLease(final String processName, final String unlockKey, final LocalDateTime until) throws ProcessIsLockedException {
        checkStatus();
        doWithRetry((Supplier<Void>) () -> {
            final LocalDateTime normalizedUntil = normalizeDate(until);
            final LockState result = lockStorage.renewLease(processName, unlockKey, normalizedUntil);

            if (result == LockState.AVAILABLE) {
                final String msg = "Failed renew lock, process is locked with different unlock key, or lock does not exist!";
                log.error(msg);
                throw new RuntimeException(new ProcessIsLockedException(msg));
            }
            return null;
        }, 3000L, 20);
    }

    /**
     * Release lock you are owner of. Ownership is based on unlock key.
     *
     * @param processName name of the process we want to have the lock renewed
     * @param unlockKey key obtained during lock leasing
     * @throws ProcessIsLockedException when the lock key doesn't match the current lock for the process
     */
    public void releaseProcess(final String processName, final String unlockKey) throws ProcessIsLockedException {
        checkStatus();
        doWithRetry((Supplier<Void>) () -> {
            if (processName == null) {
                String msg = "Cannot release process without a processName " +
                        "(method was called with null processName).";
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
            if (unlockKey == null) {
                String msg = "Cannot release process without an unlockKey (method was called with null unlockKey).";
                log.error(msg);
                throw new IllegalArgumentException(msg);
            }
            processMap.remove(processName + unlockKey);
            final LockState lockState = lockStorage.releaseProcess(processName, unlockKey);
            Assert.isTrue(lockState == LockState.LEASED);
            return null;
        }, 3000L, 20);
    }

    /**
     * Inits timerTask for checking locker.
     */
    private void setupCheckLockTimerTask(String processName, LocalDateTime until, LockRestorer lockerRestorer,
                                         LocalDateTime now, String unlockKey) {
        if (lockerRestorer != null) {
            final long delayTime = getDelayTimeInMilliseconds(now, until);
            final long renewTime = Duration.between(now, until).get(ChronoUnit.MILLIS);
            processMap.put(processName + unlockKey, lockerRestorer);
            final CheckLockTimerTask timerTask = new CheckLockTimerTask(this, processName, unlockKey, renewTime);
            if (applicationContext.containsBean("cpsModuleScheduledExecutor")) {
                /* scheduled executor is now provided by root cps context. Runtime resolving is done mainly for testing purposes   */
                applicationContext.getBean("cpsModuleScheduledExecutor", ScheduledExecutorService.class)
                        .scheduleAtFixedRate(timerTask, delayTime, renewTime, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Get delay time when timer check process
     *
     * @param now   created time in milliseconds
     * @param until expired time of lock in milliseconds
     * @return delay time in millisecond. Minimal value is 0.
     */
    private long getDelayTimeInMilliseconds(LocalDateTime now, LocalDateTime until) {
        long millis = (long) (Duration.between(until, now).get(ChronoUnit.MILLIS) * CHECK_RENEW_RATIO);
        if (millis > 0) {
            return millis;
        }
        return 0;
    }

    /**
     * Check whether there is existing non-expired lock for particular processName.
     *
     * @param processName name of the process we want to have the lock renewed
     * @throws ProcessIsLockedException if there is valid lock for this process
     */
    private void checkExistingLock(String processName) throws ProcessIsLockedException {
        checkStatus();
        final LockState lockState = lockStorage.getProcessLock(processName, normalizeDate(LocalDateTime.now()));
        if (lockState == LockState.LEASED) {
            final String msg = "Process " + processName + " has a foreign valid lock. Cannot register new one!";
            log.info(msg);
            throw new ProcessIsLockedException(msg);
        } else if (lockState == LockState.LEASED_EXPIRED) {
            if (log.isDebugEnabled()) {
                log.debug("Releasing expired lock for process " + processName);
            }
            final LockState lockState1 = lockStorage.releaseProcess(processName, null);
            Assert.isTrue(lockState1 == LockState.LEASED);
        }
    }

    /**
     * Normalizes application server date against shared database time. There may be cases when date and time
     * of the application server differs from the date and time of the database server and then until argument might
     * behave unexpectedly.
     */
    private LocalDateTime normalizeDate(LocalDateTime until) {
        final Duration diff = Duration.between(until, LocalDateTime.now());
        final LocalDateTime databaseTime = lockStorage.getCurrentDatabaseTime();
        return databaseTime.plus(diff);
    }

    /**
     * Checks whether locker is not switched off.
     */
    private void checkStatus() {
        if (switchOff) {
            String msg = "Locker is switched off - no data source accessible.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }
    }

    /**
     * Executes passed logic. If process is already leased it waits for specified amount of milliseconds and tries logic
     * again. If it fails again it repeats at most X times (as specified in `times` argument). If passed logic finishes
     * with other than {@link ProcessIsLockedException} exception, exception is immediately thrown.
     */
    private <T> T doWithRetry(Supplier<T> supplier, long waitForLockInMilliseconds, int times) throws ProcessIsLockedException {
        for (int i = 1; i <= times; i++) {
            try {

                return supplier.get();

            } catch (RuntimeException ex) {
                if (ex.getCause() instanceof ProcessIsLockedException) {
                    log.info("Lock is already leased, waiting " + waitForLockInMilliseconds +
                            " milliseconds to get it.");
                } else {
                    log.warn("Exception was returned: " + ex.getMessage() + ". Waiting " +
                            waitForLockInMilliseconds + " milliseconds to retry the attempt.");
                }
                //when process cannot be leased, wait for lock specified time
                try {
                    Thread.sleep(waitForLockInMilliseconds);
                } catch (InterruptedException e) {
                    //continue
                }

                if (i >= times) {
                    //too many attempts
                    if (ex.getCause() instanceof ProcessIsLockedException) {
                        throw (ProcessIsLockedException)ex.getCause();
                    } else {
                        throw ex;
                    }
                }
            }
        }

        throw new IllegalStateException(
                "Not expected to reach there - either exception should be already thrown or result should be returned!"
        );
    }

}