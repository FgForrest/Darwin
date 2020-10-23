UPDATE T_DB_AUTOUPDATE_PATCH SET patchName = ?,componentName = ?,platform = ?, processTime = ?, finishedOn = ?
WHERE patchName = ? AND componentName = ? AND platform = ?;