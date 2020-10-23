CREATE TABLE TEST (
  test int
);

SET @sortOrder = -1;
SET @sortOrder = 2;
SET @sortOrder = -1;

INSERT INTO TEST VALUES (@sortOrder);