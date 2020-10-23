INSERT INTO T_DB_AUTOUPDATE_PATCH (componentName, patchName, processTime, detectedOn, finishedOn, platform)
VALUES ('lib_db_autoupdate', 'BrokenFirst', 0, now(), now(), 'h2');
CREATE TABLE TEST
(
test int
);