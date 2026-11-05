delete from DARWIN_LOCK where processName = ? and (unlockKey = ? or (? is null and leaseUntil <= SYSDATE));
