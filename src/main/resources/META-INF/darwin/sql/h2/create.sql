create table DARWIN
(
  component VARCHAR2(255) not null,
  modified TIMESTAMP not null,
  version VARCHAR2(20) null
);

create table DARWIN_LOCK
(
  processName VARCHAR2(255) not null,
  leaseUntil TIMESTAMP not null,
  unlockKey VARCHAR(255) not null,
  constraint CNUN_DARWIN_LOCK unique (processName)
);

CREATE TABLE DARWIN_PATCH
(
    id INT(11)  NOT NULL auto_increment,
    componentName VARCHAR(255) NOT NULL,
    patchName VARCHAR(100) NOT NULL,
    processTime INT(11),
    detectedOn TIMESTAMP NOT NULL,
    finishedOn TIMESTAMP,
    platform VARCHAR(10),
    CONSTRAINT CNFK_PATCH_DARWIN FOREIGN KEY (componentName) REFERENCES DARWIN (component) ON DELETE CASCADE
);

CREATE INDEX CNFK_PATCH_DARWIN ON DARWIN_PATCH (componentName);
CREATE UNIQUE INDEX CNUN_ID ON DARWIN_PATCH (id);
CREATE UNIQUE INDEX CNUN_UNIQUE_PATCH ON DARWIN_PATCH (patchName, componentName, platform);

CREATE TABLE DARWIN_SQL
(
    id INT(11) NOT NULL auto_increment,
    patchId INT(11) NOT NULL,
    statementHash VARCHAR(64) NULL,
    statement MEDIUMTEXT NOT NULL,
    processTime INT(11),
    finishedOn TIMESTAMP,
    exception MEDIUMTEXT,
    CONSTRAINT CNPK_ID PRIMARY KEY (id),
    CONSTRAINT CNFK_SQL_PATCH FOREIGN KEY (patchId) REFERENCES DARWIN_PATCH (id) ON DELETE CASCADE
);
CREATE INDEX CNPK_ID ON DARWIN_SQL (patchId);
CREATE INDEX IX_DARWIN_SQL_HASH ON DARWIN_SQL (statementHash);