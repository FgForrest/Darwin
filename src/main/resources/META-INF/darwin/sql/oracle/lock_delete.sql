delete from DARWIN_LOCK where processName = ? and (leaseUntil <= SYSDATE or unlockKey = ?);