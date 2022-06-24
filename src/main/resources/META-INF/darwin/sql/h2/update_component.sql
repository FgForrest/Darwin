update DARWIN set
 modified = CURRENT_TIMESTAMP(),
 version = ?
where component = ?;
