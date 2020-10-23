create table T_DB_AUTOUPDATE
(
	DB_AUTOUPDATE_IN_PK integer not null auto_increment,
	COMPONENT_TX varchar(255) not null,
	MODIFIED_DT datetime not null,
	VERSION_TX varchar(20) null,
	constraint CNPK_DB_AUTOUPDATE primary key (DB_AUTOUPDATE_IN_PK),
	index IX_DB_AUTOUPDATE_COMPONENT (COMPONENT_TX)
) engine=InnoDB;


