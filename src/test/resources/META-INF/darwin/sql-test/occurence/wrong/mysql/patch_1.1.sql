CREATE TABLE TEST (
  test int
);

SET @sortOrder = -1;
SET @sortOrder = 2;
SET @sortOrder = -1;

ERROR;

INSERT INTO TEST VALUES (@sortOrder);