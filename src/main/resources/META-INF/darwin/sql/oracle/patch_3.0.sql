ALTER TABLE T_DB_AUTOUPDATE ADD CONSTRAINT CNPK_AUTOUPDATE_COMPONENT PRIMARY KEY (COMPONENT_TX);

CREATE TABLE T_DB_AUTOUPDATE_PATCH
(
  id INT NOT NULL,
  componentName VARCHAR(255) NOT NULL,
  patchName VARCHAR(100) NOT NULL,
  processTime INT,
  detectedOn DATE NOT NULL,
  finishedOn DATE,
  platform VARCHAR(10),
  CONSTRAINT CNPK_AUTOUPDATE_PATCH_ID PRIMARY KEY (id),
  CONSTRAINT CNFK_PACH_AUTOUPDATE FOREIGN KEY (componentName) REFERENCES T_DB_AUTOUPDATE (COMPONENT_TX) ON DELETE CASCADE
);
CREATE INDEX CNIX_T_DB_AUTOUPDATE_PATCH ON T_DB_AUTOUPDATE_PATCH (componentName);
CREATE UNIQUE INDEX CNUN_UNIQUE_PATCH ON T_DB_AUTOUPDATE_PATCH (patchName, componentName, platform);

CREATE SEQUENCE SQ_T_DB_AUTOUPDATE_PATCH
MINVALUE 1
START WITH 1
INCREMENT BY 1
NOCACHE;

CREATE TABLE T_DB_AUTOUPDATE_SQL
(
    id INT NOT NULL,
    patchId INT NOT NULL,
    statement CLOB NOT NULL,
    processTime INT,
    finishedOn DATE,
    exception CLOB,
    CONSTRAINT CNPK_AUTOUPDATE_SQL_ID PRIMARY KEY (id),
    CONSTRAINT CNFK_SQL_PATCH FOREIGN KEY (patchId) REFERENCES T_DB_AUTOUPDATE_PATCH (id) ON DELETE CASCADE
);
CREATE INDEX CNIX_T_DB_AUTOUPDATE_SQL ON T_DB_AUTOUPDATE_SQL (patchId);

CREATE SEQUENCE SQ_T_DB_AUTOUPDATE_SQL
MINVALUE 1
START WITH 1
INCREMENT BY 1
NOCACHE;

INSERT INTO T_DB_AUTOUPDATE_PATCH (id,componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUES (SQ_T_DB_AUTOUPDATE_PATCH.nextval,'lib_db_autoupdate','patch_3.0.sql',15,to_date('2016-12-10', 'yyyy-mm-dd'),to_date('2016-12-10', 'yyyy-mm-dd'),'oracle');
INSERT INTO T_DB_AUTOUPDATE_PATCH (id,componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUES (SQ_T_DB_AUTOUPDATE_PATCH.nextval,'lib_db_autoupdate','patch_1.1.sql',15,to_date('2016-12-10', 'yyyy-mm-dd'),to_date('2016-12-10', 'yyyy-mm-dd'),'oracle');
INSERT INTO T_DB_AUTOUPDATE_PATCH (id,componentName,patchName,processTime,detectedOn,finishedOn,platform) VALUES (SQ_T_DB_AUTOUPDATE_PATCH.nextval,'lib_db_autoupdate','create.sql',15,to_date('2016-12-10', 'yyyy-mm-dd'),to_date('2016-12-10', 'yyyy-mm-dd'),'oracle');
