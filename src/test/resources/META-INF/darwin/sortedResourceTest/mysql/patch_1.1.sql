create table DARWIN_LOCK
(
	id integer not null auto_increment,
	processName varchar(255) not null,
	leaseUntil datetime not null,
	unlockKey varchar(255) not null,
	constraint DARWIN_LOCK primary key (id),
	constraint CNUN_DB_AUTOUPDATE_LOCK unique (processName)
) engine=InnoDB;

