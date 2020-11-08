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

Lock is renewed as long as:

- the maximum number of lock extensions has been reached (the maximum number is defined by the constant MAX_RENEW_COUNT = 10)
- the lock has already been unlocked using the releaseProcess method
- the process signalizes TRUE in method lockRestorer.isFinished()

CheckLockerTimerTask is triggered after 70% of the lock validity (eg if the lock validity is set to 10min, the
lock is extended after 7 minutes). There is no hard guarantee the CheckLockerTimerTask will be invoked by the system.
It uses standard `java.util.concurrent.ScheduledExecutorService.scheduleAtFixedRate` which may not invoke tasks when
system is under pressure.