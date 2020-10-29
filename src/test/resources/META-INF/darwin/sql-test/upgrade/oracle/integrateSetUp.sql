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
  unlockKey varchar(255) not null,
  constraint CNUN_DB_AUTOUPDATE_LOCK unique (processName)
);
insert into DARWIN (component, modified, version) values ('darwin', SYSDATE, '1.1');
insert into DARWIN (component, modified, version) values ('testovaci', SYSDATE, '3.1');
