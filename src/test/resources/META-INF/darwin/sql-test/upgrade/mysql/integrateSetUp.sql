CREATE TABLE DARWIN
(
  id INTEGER      NOT NULL AUTO_INCREMENT,
  component        VARCHAR(255) NOT NULL,
  modified         DATETIME     NOT NULL,
  version          VARCHAR(20)  NULL,
  CONSTRAINT CNPK_DB_AUTOUPDATE PRIMARY KEY (id),
  INDEX IX_DB_AUTOUPDATE_COMPONENT (component)
)
  ENGINE = InnoDB;

CREATE TABLE DARWIN_LOCK
(
  id INTEGER      NOT NULL AUTO_INCREMENT,
  processName               VARCHAR(255) NOT NULL,
  leaseUntil           DATETIME     NOT NULL,
  unlockKey            VARCHAR(255) NOT NULL,
  CONSTRAINT DARWIN_LOCK PRIMARY KEY (id),
  CONSTRAINT CNUN_DB_AUTOUPDATE_LOCK UNIQUE (processName)
)
  ENGINE = InnoDB;

INSERT INTO DARWIN (component, modified, version) VALUES ('darwin', now(), '1.1');
INSERT INTO DARWIN (component, modified, version) VALUES ('testovaci', now(), '3.1');
