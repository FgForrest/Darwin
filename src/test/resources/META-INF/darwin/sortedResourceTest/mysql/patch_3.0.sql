CREATE TABLE DARWIN_PATCH
(
    id INT(11) NOT NULL AUTO_INCREMENT,
    componentName VARCHAR(255) NOT NULL,
    patchName VARCHAR(100) NOT NULL,
    processTime INT(11),
    detectedOn DATE NOT NULL,
    finishedOn DATE,
    platform VARCHAR(10),
    CONSTRAINT CNPK_PATCH_ID primary key (id),
    CONSTRAINT CNFK_PATCH_AUTOUPDATE FOREIGN KEY (componentName) REFERENCES DARWIN (component) ON DELETE CASCADE

);

CREATE INDEX CNIX_DARWIN_PATCH ON DARWIN_PATCH (componentName);
CREATE UNIQUE INDEX CNUN_id_UNIQUE ON DARWIN_PATCH (id);
CREATE UNIQUE INDEX CNUN_UNIQUE_PATCH ON DARWIN_PATCH (patchName, componentName, platform);

CREATE TABLE DARWIN_SQL
(
    id INT(11) NOT NULL AUTO_INCREMENT,
    patchId INT(11) NOT NULL,
    statement MEDIUMTEXT NOT NULL,
    processTime INT(11),
    finishedOn DATETIME,
    exception MEDIUMTEXT,
    CONSTRAINT CNPK_SQL_ID PRIMARY KEY (id),
    CONSTRAINT CNFK_SQL_PATCH FOREIGN KEY (patchId) REFERENCES DARWIN_PATCH (id) ON DELETE CASCADE
);
CREATE INDEX CNIX_DARWIN_SQL ON DARWIN_SQL (patchId);

INSERT INTO DARWIN_PATCH (componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUE ('darwin','patch_3.0.sql',0,'2016-06-28','2016-06-28','mysql');
INSERT INTO DARWIN_PATCH (componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUE ('darwin','patch_1.1.sql',0,'2016-06-28','2016-06-28','mysql');
INSERT INTO DARWIN_PATCH (componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUE ('darwin','create.sql',0,'2016-06-28','2016-06-28','mysql');
