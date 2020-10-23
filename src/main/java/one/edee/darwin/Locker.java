package one.edee.darwin;

import lombok.Data;
import lombok.extern.apachecommons.CommonsLog;
import one.edee.darwin.exception.ProcessIsLockedException;
import one.edee.darwin.locker.CheckLockTimerTask;
import one.edee.darwin.locker.LockRestorer;
import one.edee.darwin.resources.ResourceAccessor;
import one.edee.darwin.storage.DefaultDatabaseLockPersister;
import one.edee.darwin.storage.LockPersister;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
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
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class provides functionality to manage locks for processes.
 *
 * @author Jan Novotný, FG Forrest a.s. (c) 2007
 */
@CommonsLog
@Data
public class Locker implements InitializingBean, ApplicationContextAware {

    private static final double CHECK_RENEW_RATIO = 0.7;

	/**
	 * If set to true Locker with silently disable itself in case datasource is not present in application context.
	 */
    private boolean skipIfDataSourceNotPresent = true;

    /**
     * Allows to define, whether locker creates own transaction for each DB call.
     * By default new transaction is NOT created.
     *
     * Set <code>true</code> if separate transaction should be used
     */
    private boolean inSeparateTransaction;

	private final Map<String, LockRestorer> processMap = new ConcurrentHashMap<>();
	private boolean switchOff;
	private String dataSourceName = "dataSource";
	private String transactionManagerName = "transactionManager";

    private ApplicationContext ctx;
    private LockPersister lockPersister;
    private ResourceAccessor resourceAccessor;

    /**
     * create lock on specific dataSource
     *
     * @param ds DataSource
     */
    public static LockPersister createDefaultLockPersister(DataSource ds, PlatformTransactionManager transactionManager,
                                                           ResourceAccessor resourceAccessor,
														   ResourceLoader resourceLoader,
                                                           boolean inSeparateTransaction) {
        DefaultDatabaseLockPersister lockPersister = new DefaultDatabaseLockPersister();
        lockPersister.setResourceAccessor(resourceAccessor);
        lockPersister.setDataSource(ds);
        lockPersister.setTransactionManager(transactionManager);
		lockPersister.setResourceLoader(resourceLoader);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        if (inSeparateTransaction) {
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
        } else {
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        }
        lockPersister.setTransactionTemplate(transactionTemplate);

        return lockPersister;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(resourceAccessor, "Locker needs resourceAccessor property to be not null!");

        //defaults
        boolean dataSourcePresent = ((AbstractApplicationContext) ctx).getBeanFactory().containsBean(dataSourceName);
        boolean transactionManagerPresent =
                ((AbstractApplicationContext) ctx).getBeanFactory().containsBean(transactionManagerName);
        if (dataSourcePresent) {
            DataSource ds = (DataSource) ctx.getBean(dataSourceName);
            PlatformTransactionManager transactionManager = transactionManagerPresent ?
                    (PlatformTransactionManager) ctx.getBean(transactionManagerName) : null;
            if (lockPersister == null) {
                lockPersister = createDefaultLockPersister(
                		ds, transactionManager, resourceAccessor,
                        ctx, inSeparateTransaction
				);
            }
        } else {
            if (!skipIfDataSourceNotPresent) {
                throw new IllegalStateException("DataSource not accessible and skipIfDataSourceNotPresent " +
                        "flag is not set. Cannot perform database locking.");
            } else {
                switchOff = true;
            }
        }
    }

    /**
     * Returns true if process can be leased. Result is not guaranteed though - method may return TRUE at one time
     * and event then leasing attempt in the next second may fail.
     */
    public boolean canLease(String processName) {
        return lockPersister.getProcessLock(processName, normalizeDate(new Date())) != 1;
    }

    /**
     * Method stores lock on particular process.
     *
     * @param processName name of the process we want to lock
     * @param until       date until lock should be kept providing no one has unlock it by then
     * @return key for unlocking stored lock
     */
    public String leaseProcess(final String processName, final Date until, int waitForLockInMiliseconds)
            throws ProcessIsLockedException {
        return doWithRetry(new Supplier<String>() {
            @Override
            public String get() {
                try {
                    return leaseProcess(processName, until);
                } catch (ProcessIsLockedException e) {
                    throw new RuntimeException(e);
                }
            }
        }, waitForLockInMiliseconds, 10);
    }

    /**
     * Method stores lock on particular process.
     *
     * @param processName name of the process we want to lock
     * @param until       date until lock should be kept providing no one has unlock it by then
     * @return key for unlocking stored lock
     */
    public String leaseProcess(String processName, Date until) throws ProcessIsLockedException {
        checkStatus();
        //verify there is no lock
        checkExistingLock(processName);

        try {
            until = normalizeDate(until);
            String unlockKey = Long.toHexString(System.currentTimeMillis());
            lockPersister.createLock(processName, until, unlockKey);

            if (log.isDebugEnabled()) {
                SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                log.debug("Process " + processName + " locked with unlockKey " + unlockKey +
                        " until " + fmt.format(until) + "");
            }

            return unlockKey;
        } catch (DataIntegrityViolationException ex) {
            //Issue #6074 - create lock could happen simultaneously - in that case throw ProcessIsLockedException
            String msg = "Process " + processName + " has a foreign valid lock. Cannot register new one!";
            log.warn(msg);
            throw new ProcessIsLockedException(msg);
        }

    }

    /**
     * Method stores lock on particular process and start lock restorer
     *
     * @param processName name of the process we want to lock
     * @param until       date until lock should be kept providing no one has unlock it by then
     * @throws ProcessIsLockedException
     */
    public String leaseProcess(String processName, Date until, LockRestorer lockerRestorer)
            throws ProcessIsLockedException {
        long now = System.currentTimeMillis();

        String unlockKey = leaseProcess(processName, until);

        setupCheckLockTimerTask(processName, until, lockerRestorer, now, unlockKey);
        return unlockKey;
    }

    /**
     * Method stores lock on particular process and start lock restorer
     *
     * @param processName name of the process we want to lock
     * @param until       date until lock should be kept providing no one has unlock it by then
     * @return key for unlocking stored lock
     * @throws ProcessIsLockedException
     */
    public String leaseProcess(String processName, Date until, int waitForLockInMiliseconds,
                               LockRestorer lockerRestorer) throws ProcessIsLockedException {
        long now = System.currentTimeMillis();

        String unlockKey = leaseProcess(processName, until, waitForLockInMiliseconds);

        setupCheckLockTimerTask(processName, until, lockerRestorer, now, unlockKey);
        return unlockKey;
    }

    /**
     * Renews lease date for particular process, if you have correct unlock key (otherwise exeption is thrown)
     *
     * @throws ProcessIsLockedException
     */
    public void renewLease(final String processName, final String unlockKey, final Date until)
            throws ProcessIsLockedException {
        checkStatus();
        doWithRetry(new Supplier<Void>() {
            @Override
            public Void get() {
                Date normalizedUntil = normalizeDate(until);
                int result = lockPersister.renewLease(processName, unlockKey, normalizedUntil);

                if (result == 0) {
                    String msg = "Failed renew lock, process is locked with different unlock key," +
                            " or lock does not exist!";
                    log.error(msg);
                    throw new RuntimeException(new ProcessIsLockedException(msg));
                }
                return null;
            }
        }, 3000L, 20);
    }

    /**
     * Release lock you are owner of. Ownership is based on unlockKey
     * @Deprecated typo in method name, use {@link #releaseProcess(String, String)}
     */
    public void releaseProces(final String processName, final String unlockKey) {
        releaseProcess(processName,unlockKey);
    }

    /**
     * Release lock you are owner of. Ownership is based on unlockKey
     */
    public void releaseProcess(final String processName, final String unlockKey) {
        checkStatus();
        doWithRetry(new Supplier<Void>() {
            @Override
            public Void get() {
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
                int result = lockPersister.releaseProcess(processName, unlockKey);
                processMap.remove(processName + unlockKey);
                if (result == 0) {
                    String msg = "No lock for process" + processName + " and unlockKey " + unlockKey + " was found!";
                    log.error(msg);
                    throw new IllegalStateException(msg);
                }
                return null;
            }
        }, 3000L, 20);
    }

    /**
     * Init timerTask for checking locker
     *
     * @param processName name of the process we want to lock
     * @param until       date until lock should be kept providing no one has unlock it by then
     */
    private void setupCheckLockTimerTask(String processName, Date until, LockRestorer lockerRestorer,
                                         long now, String unlockKey) {
        if (lockerRestorer != null) {
            long delayTime = getDelayTime(now, until);
            long renewTime = until.getTime() - now;
            processMap.put(processName + unlockKey, lockerRestorer);
            CheckLockTimerTask timerTask = new CheckLockTimerTask(this, processName, unlockKey, renewTime);
            if (ctx.containsBean("cpsModuleScheduledExecutor")) {
                /* scheduled executor is now provided by root cps context. Runtime resolving is done mainly for testing purposes   */
                ctx.getBean("cpsModuleScheduledExecutor",
                        ScheduledExecutorService.class).scheduleAtFixedRate(timerTask, delayTime, renewTime,
                        TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Get delay time when timer check process
     *
     * @param now   created time in miliseconds
     * @param until expired time of lock in miliseconds
     * @return delay time in milisecond. Minimal value is 0.
     */
    private long getDelayTime(long now, Date until) {
        long result = (long) ((until.getTime() - now) * CHECK_RENEW_RATIO);
        if (result > 0) {
            return result;
        }
        return 0;
    }

    /**
     * Check whether there is existing nonexpired lock for particular processName.
     *
     * @throws ProcessIsLockedException
     */
    private void checkExistingLock(String processName) throws ProcessIsLockedException {
        checkStatus();
        int lockState = lockPersister.getProcessLock(processName, normalizeDate(new Date()));
        if (lockState == 1) {
            String msg = "Process " + processName + " has a foreign valid lock. Cannot register new one!";
            log.info(msg);
            throw new ProcessIsLockedException(msg);
        } else if (lockState == 2) {
            if (log.isDebugEnabled()) {
                log.debug("Releasing expired lock for process " + processName);
            }
            lockPersister.releaseProcess(processName, null);
        }
    }

    /**
     * Normalizes application server date against shared database time.
     */
    private Date normalizeDate(Date until) {
        long diff = until.getTime() - new Date().getTime();
        Date databaseTime = lockPersister.getCurrentDatabaseTime();
        return new Date(databaseTime.getTime() + diff);
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

    private <T> T doWithRetry(Supplier<T> supplier, long waitForLockInMiliseconds, int times) {
        for (int i = 1; i <= times; i++) {
            try {

                return supplier.get();

            } catch (RuntimeException ex) {
                if (ex.getCause() instanceof ProcessIsLockedException) {
                    log.info("Lock is already leased, waiting " + waitForLockInMiliseconds +
                            " milliseconds to get it.");
                } else {
                    log.warn("Exception was returned: " + ex.getMessage() + ". Waiting " +
                            waitForLockInMiliseconds + " milliseconds to retry the attempt.");
                }
                //when process cannot be leased, wait for lock specified time
                try {
                    Thread.sleep(waitForLockInMiliseconds);
                } catch (InterruptedException e) {
                    //continue
                }

                if (i >= times) {
                    //too many attempts
                    throw ex;
                }
            }
        }

        throw new IllegalStateException(
                "Not expected to reach there - either exception should be already thrown or result should be returned!"
        );
    }

    private interface Supplier<T> {

        T get();

    }

}