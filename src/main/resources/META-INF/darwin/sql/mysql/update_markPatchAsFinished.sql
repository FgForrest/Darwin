UPDATE DARWIN_PATCH
SET finishedOn = ?
WHERE patchName = ? AND componentName = ? AND platform = ?;