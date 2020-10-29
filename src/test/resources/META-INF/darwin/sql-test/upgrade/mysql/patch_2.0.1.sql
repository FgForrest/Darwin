UPDATE DARWIN_PATCH SET finishedOn = now() WHERE patchName = 'create.sql' AND componentName = 'testovaci' AND platform = 'mysql';
UPDATE DARWIN_PATCH SET finishedOn = now() WHERE patchName = 'patch_1.1.sql' AND componentName = 'testovaci' AND platform = 'mysql';
UPDATE DARWIN_PATCH SET finishedOn = now() WHERE patchName = 'patch_3.0.sql' AND componentName = 'testovaci' AND platform = 'mysql';