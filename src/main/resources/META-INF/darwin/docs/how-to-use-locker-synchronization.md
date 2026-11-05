# Process synchronization in a cluster using shared RDBMS

The Locker class has been created for the needs of Darwin, but you can use it in your application as well. Locker will 
ensure that your processes that are not written for concurrent execution are executed one at a time eve in cluster
environment with multiple JVMs. The single prerequisite is a shared relational database that is accessible from all the 
nodes.

Locker provides the following main methods:

* `String leaseProcess (String processName, Date until) throws ProcessIsLockedException`
* `void renewLease (String processName, String unlockKey, Date until) throws ProcessIsLockedException`
* `void releaseProcess (String processName, String unlockKey)`

The first method will get the lock for your process, the second method will release the lock. Locker will not allow 
acquiring two locks to the process with the same name. However, the whole principle is based on the form of leasing - 
the lock is only borrowed for the specified period (`until` argument). This leasing mechanism addresses the problem 
of unexpected application termination when the locks are not released at all. In such situation processes can recover
automatically after the application restart one the lock lease period expires.

It's recommended to specify the `until` argument to a moment that to process is certainly finished. The better yet 
to add a considerable reserve time so that the process won't exceed leased time. When process contains an inner loop for
paged records processing, it's recommended to prolong lease by calling `renewLease` method after each page has been
processed.   

At the end the process is expected to return acquired lock by passing random key acquired during lease process. For 
returning the key use `releaseProcess` method.

An expired lease cannot be renewed. Renewal requires both the current owner token and a still-valid lease, checked
atomically against database time. Lost ownership is reported immediately as `ProcessIsLockedException`; transient
storage failures in the public `renewLease` method retain the configured retry behavior. Treat the token as opaque.
An explicit release only removes the lock belonging to that token, even if another owner's lock has expired.

## Recommended usage

See following example for recommended usage:

``` java
String theKey = null;
try {
    theKey = locker.leaseProcess("myProcess", LocalDateTime.now().plusMinutes(30));
    // do your stuff
    while (pageOfRecords.hasNextPage()) {
        // process page
        locker.renewLease("myProcess", theKey, LocalDateTime.now().plusMinutes(30));
    }
} catch (ProcessIsLockedException ex) {
    // process is running somewhere else - just log it
    logger.info("Process myProcess is running elsewhere, skipping this execution and will try next time.");
} finally {
    Optional.ofNullable(theKey)
            .ifPresent(it -> locker.releaseProcess("myProcess", it));
}
```

## Automatic lock extension

If you cannot estimate the proper lease time you can take advantage of automatic asynchronous lock prolonging process. 
There are two special forms of lease methods that accept `LockRestorer` implementation:

* `String leaseProcess(String processName, Date until, LockRestorer lockerRestorer)`
* `String leaseProcess(String processName, Date until, int waitForLockInMilliseconds, LockRestorer lockRestorer)`

The last parameter represents your logic implementing the LockRestorer interface that returns flag signalizing whether
your process already finished or not.

By calling these lease methods a new instance of CheckLockTimerTask is created and scheduled and periodically calls your
LockRestorer implementation to determine whether lock needs to be prolonged.

Lock is renewed until one of the following happens:

- the maximum number of checks has been reached (`MAX_RENEW_COUNT = 29`, keeping the lock for about 10.7 lease periods)
- the lock has already been unlocked using the releaseProcess method
- the process signalizes TRUE in method lockRestorer.isFinished()

CheckLockTimerTask runs every third of the lease validity and extends the lock by the whole validity. For a 30-minute
lease it calls `LockRestorer.isFinished()` every 10 minutes. One failed storage attempt can therefore be retried at the
next check before expiration; lost ownership stops further checks. Use leases of at least several seconds because
database time may have only second precision. There is no hard guarantee the CheckLockTimerTask will be invoked by the system.
It uses standard `java.util.concurrent.ScheduledExecutorService.scheduleAtFixedRate` which may not invoke tasks when
system is under pressure.

Lock statement timeouts are opt-in via `DefaultDatabaseLockStorage.setQueryTimeoutSeconds`; by default the JDBC driver's
settings are preserved. Do not enable this option with H2 on a shared or pooled data source: H2 retains statement
timeouts on the connection and can cancel unrelated statements later. Connection acquisition is not covered by this timeout.

All applications sharing `DARWIN_LOCK` must use the fixed lease contract. Older clients still execute their original
SQL and can renew expired leases. Stopping renewal does not stop the running process or undo its external effects.
