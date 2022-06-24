create table DARWIN
(
  component VARCHAR(255) not null,
  modified TIMESTAMP not null,
  version VARCHAR(20) null
);

create table DARWIN_LOCK
(
  processName VARCHAR(255) not null,
  leaseUntil TIMESTAMP not null,
  unlockKey VARCHAR(255) not null,
  constraint CNUN_DARWIN_LOCK unique (processName)
);

CREATE TABLE DARWIN_PATCH
(
    id IDENTITY NOT NULL PRIMARY KEY,
    componentName VARCHAR(255) NOT NULL,
    patchName VARCHAR(100) NOT NULL,
    processTime INT,
    detectedOn TIMESTAMP NOT NULL,
    finishedOn TIMESTAMP,
    platform VARCHAR(10)
);

CREATE INDEX CNFK_PATCH_DARWIN ON DARWIN_PATCH (componentName);
CREATE UNIQUE INDEX CNUN_ID ON DARWIN_PATCH (id);
CREATE UNIQUE INDEX CNUN_UNIQUE_PATCH ON DARWIN_PATCH (patchName, componentName, platform);

CREATE TABLE DARWIN_SQL
(
    id IDENTITY NOT NULL PRIMARY KEY,
    patchId INT NOT NULL,
    statementHash VARCHAR(64) NULL,
    statement MEDIUMTEXT NOT NULL,
    processTime INT,
    finishedOn TIMESTAMP,
    exception MEDIUMTEXT
);
CREATE INDEX CNPK_ID ON DARWIN_SQL (patchId);
CREATE INDEX IX_DARWIN_SQL_HASH ON DARWIN_SQL (statementHash);