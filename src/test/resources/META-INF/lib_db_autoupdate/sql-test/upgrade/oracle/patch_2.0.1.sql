UPDATE T_DB_AUTOUPDATE_PATCH
SET finishedOn = SYSDATE
WHERE patchName = 'create.sql' AND componentName = 'testovaci' AND platform = 'oracle';
UPDATE T_DB_AUTOUPDATE_PATCH
SET finishedOn = SYSDATE
WHERE patchName = 'patch_1.1.sql' AND componentName = 'testovaci' AND platform = 'oracle';
UPDATE T_DB_AUTOUPDATE_PATCH
SET finishedOn = SYSDATE
WHERE patchName = 'patch_3.0.sql' AND componentName = 'testovaci' AND platform = 'oracle';