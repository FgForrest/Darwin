create table T_DB_AUTOUPDATE_LOCK
(
	DB_AUTOUPDATE_LOCK_IN_PK integer not null auto_increment,
	PROCESS_TX varchar(255) not null,
	LEASE_UNTIL_DT datetime not null,
	UNLOCK_KEY_TX varchar(255) not null,
	constraint T_DB_AUTOUPDATE_LOCK primary key (DB_AUTOUPDATE_LOCK_IN_PK),
	constraint CNUN_DB_AUTOUPDATE_LOCK unique (PROCESS_TX)
) engine=InnoDB;

