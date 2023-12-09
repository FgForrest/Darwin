create table DARWIN
(
	id INT(11) not null auto_increment,
	component varchar(255) not null,
	modified datetime not null,
	version varchar(20) null,
	constraint CNPK_DARWIN primary key (id),
	index IX_DARWIN_COMPONENT (component)
) engine=InnoDB;

create table DARWIN_LOCK
(
	id INT(11) not null auto_increment,
	processName varchar(255) not null,
	leaseUntil datetime not null,
	unlockKey varchar(255) not null,
	constraint DARWIN_LOCK primary key (id),
	constraint CNUN_DARWIN_LOCK unique (processName)
) engine=InnoDB;

CREATE TABLE DARWIN_PATCH
(
    id INT(11) NOT NULL AUTO_INCREMENT,
    componentName VARCHAR(255) NOT NULL,
    patchName VARCHAR(100) NOT NULL,
    processTime INT(11),
    detectedOn DATETIME NOT NULL,
    finishedOn DATETIME,
    platform VARCHAR(10),
    CONSTRAINT CNPK_PATCH_ID primary key (id),
    CONSTRAINT CNFK_PATCH_DARWIN FOREIGN KEY (componentName) REFERENCES DARWIN (component) ON DELETE CASCADE

) engine=InnoDB;

CREATE INDEX CNIX_DARWIN_PATCH ON DARWIN_PATCH (componentName);
CREATE UNIQUE INDEX CNUN_DARWIN_id_UNIQUE ON DARWIN_PATCH (id);
CREATE UNIQUE INDEX CNUN_DARWIN_UNIQUE_PATCH ON DARWIN_PATCH (patchName, componentName, platform);

CREATE TABLE DARWIN_SQL
(
    id INT(11) NOT NULL AUTO_INCREMENT,
    patchId INT(11) NOT NULL,
    statementHash VARCHAR(64) NULL,
    statement MEDIUMTEXT NOT NULL,
    processTime INT(11),
    finishedOn DATETIME,
    exception MEDIUMTEXT,
    CONSTRAINT CNPK_DARWIN_SQL_ID PRIMARY KEY (id),
    CONSTRAINT CNFK_DARWIN_SQL_PATCH FOREIGN KEY (patchId) REFERENCES DARWIN_PATCH (id) ON DELETE CASCADE
) engine=InnoDB;

CREATE INDEX CNIX_DARWIN_SQL ON DARWIN_SQL (patchId);
CREATE INDEX IX_DARWIN_SQL_HASH ON DARWIN_SQL (statementHash);
