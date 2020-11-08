# Script content rules

Script contains one or multiple SQL statements delimited by semicolon (;). In other words scripts follow the SQL
standard. 

If you need to escape semicolons - for example in order to create trigger or procedure, double them. See example:

``` sql
DELIMITER $$;

CREATE TRIGGER TR_DENY_SUBJECT_DELETION
BEFORE DELETE
ON T_FRAUS_SUBJECT
FOR EACH ROW
  BEGIN
    IF OLD.id is not null THEN
      SIGNAL SQLSTATE '50000'
      SET MESSAGE_TEXT = 'Row cannot be removed';;
    END IF;;
  END
$$;

DELIMITER ;;;
```

You can use standard comment blocks in your script:

``` sql
-- single line comment
-- or even single line comment in the middle of the line:
SELECT * FROM Customers -- WHERE (CustomerName LIKE 'L%')

/* multi
   line
   comment */

-- or inner comment:
SELECT * FROM Customers WHERE (CustomerName LIKE 'L%'
OR CustomerName LIKE 'R%' /*OR CustomerName LIKE 'S%'
OR CustomerName LIKE 'T%'*/ OR CustomerName LIKE 'W%')
AND Country='USA'
ORDER BY CustomerName;
```