create table T_DB_AUTOUPDATE_LOCK 
(
  PROCESS_TX VARCHAR2(255) not null,
  LEASE_UNTIL_DT TIMESTAMP not null,
  UNLOCK_KEY_TX varchar(255) not null,
  constraint CNUN_DB_AUTOUPDATE_LOCK unique (PROCESS_TX)    
);