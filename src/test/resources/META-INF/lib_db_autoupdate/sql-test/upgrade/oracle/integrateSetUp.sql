create table T_DB_AUTOUPDATE
(
  COMPONENT_TX VARCHAR2(255) not null,
  MODIFIED_DT TIMESTAMP not null,
  VERSION_TX VARCHAR2(20) null
);

create table T_DB_AUTOUPDATE_LOCK
(
  PROCESS_TX VARCHAR2(255) not null,
  LEASE_UNTIL_DT TIMESTAMP not null,
  UNLOCK_KEY_TX varchar(255) not null,
  constraint CNUN_DB_AUTOUPDATE_LOCK unique (PROCESS_TX)
);
insert into T_DB_AUTOUPDATE (COMPONENT_TX, MODIFIED_DT, VERSION_TX) values ('lib_db_autoupdate', SYSDATE, '1.1');
insert into T_DB_AUTOUPDATE (COMPONENT_TX, MODIFIED_DT, VERSION_TX) values ('testovaci', SYSDATE, '3.1');
