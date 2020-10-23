CREATE TABLE T_DB_AUTOUPDATE_PATCH
(
    id INT(11) NOT NULL AUTO_INCREMENT,
    componentName VARCHAR(255) NOT NULL,
    patchName VARCHAR(100) NOT NULL,
    processTime INT(11),
    detectedOn DATE NOT NULL,
    finishedOn DATE,
    platform VARCHAR(10),
    CONSTRAINT CNPK_PATCH_ID primary key (id),
    CONSTRAINT CNFK_PATCH_AUTOUPDATE FOREIGN KEY (componentName) REFERENCES T_DB_AUTOUPDATE (COMPONENT_TX) ON DELETE CASCADE

);

CREATE INDEX CNIX_T_DB_AUTOUPDATE_PATCH ON T_DB_AUTOUPDATE_PATCH (componentName);
CREATE UNIQUE INDEX CNUN_id_UNIQUE ON T_DB_AUTOUPDATE_PATCH (id);
CREATE UNIQUE INDEX CNUN_UNIQUE_PATCH ON T_DB_AUTOUPDATE_PATCH (patchName, componentName, platform);

CREATE TABLE T_DB_AUTOUPDATE_SQL
(
    id INT(11) NOT NULL AUTO_INCREMENT,
    patchId INT(11) NOT NULL,
    statement MEDIUMTEXT NOT NULL,
    processTime INT(11),
    finishedOn DATETIME,
    exception MEDIUMTEXT,
    CONSTRAINT CNPK_SQL_ID PRIMARY KEY (id),
    CONSTRAINT CNFK_SQL_PATCH FOREIGN KEY (patchId) REFERENCES T_DB_AUTOUPDATE_PATCH (id) ON DELETE CASCADE
);
CREATE INDEX CNIX_T_DB_AUTOUPDATE_SQL ON T_DB_AUTOUPDATE_SQL (patchId);

INSERT INTO T_DB_AUTOUPDATE_PATCH (componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUE ('lib_db_autoupdate','patch_3.0.sql',0,'2016-06-28','2016-06-28','mysql');
INSERT INTO T_DB_AUTOUPDATE_PATCH (componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUE ('lib_db_autoupdate','patch_1.1.sql',0,'2016-06-28','2016-06-28','mysql');
INSERT INTO T_DB_AUTOUPDATE_PATCH (componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUE ('lib_db_autoupdate','create.sql',0,'2016-06-28','2016-06-28','mysql');
