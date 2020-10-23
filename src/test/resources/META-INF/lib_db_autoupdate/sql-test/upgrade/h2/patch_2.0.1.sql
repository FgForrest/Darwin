UPDATE T_DB_AUTOUPDATE_PATCH
SET finishedOn = now()
WHERE patchName = 'create.sql' AND componentName = 'testovaci' AND platform = 'h2';
UPDATE T_DB_AUTOUPDATE_PATCH
SET finishedOn = now()
WHERE patchName = 'patch_1.1.sql' AND componentName = 'testovaci' AND platform = 'h2';
UPDATE T_DB_AUTOUPDATE_PATCH
SET finishedOn = now()
WHERE patchName = 'patch_3.0.sql' AND componentName = 'testovaci' AND platform = 'h2';