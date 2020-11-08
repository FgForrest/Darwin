create table ${tablePrefix}DARWIN
(
	id integer not null auto_increment,
	component varchar(255) not null,
	modified datetime not null,
	version varchar(20) null,
	constraint CNPK_DB_AUTOUPDATE primary key (id),
	index IX_DB_AUTOUPDATE_COMPONENT (component)
) engine=InnoDB;