UPDATE T_DB_AUTOUPDATE_PATCH
SET finishedOn = ?
WHERE patchName = ? AND componentName = ? AND platform = ?;