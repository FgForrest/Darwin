update T_DB_AUTOUPDATE set
 MODIFIED_DT = now(),
 VERSION_TX = ?
where COMPONENT_TX = ?;