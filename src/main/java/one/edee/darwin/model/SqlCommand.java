package one.edee.darwin.model;

import lombok.Data;
import one.edee.darwin.exception.SqlCommandFormatException;

import java.util.Date;

/**
 * This class represents single SQL statement that is part of the {@link Patch} and should be applied in order to evolve
 * database model to current version.
 *
 * @author Radek Salay, FG Forest a.s. 6/15/16.
 */
@Data
public class SqlCommand {
    private final int patchId;
    private final String statement;
    private final long processTime;
    private final Date finishedOn;
    private Exception exception;

    /**
     * Constructor.
     *
     * @param patchId     id patches which include sql
     * @param statement   sql command him self
     * @param processTime time in milliseconds of how long the application of the statement in DB took
     * @param finishedOn  date and time when the SQL statement was finished
     */
    public SqlCommand(int patchId, String statement, long processTime, Date finishedOn) {
        validateSqlCommand(patchId, statement, processTime);
        this.patchId = patchId;
        this.statement = statement;
        this.processTime = processTime;
        this.finishedOn = finishedOn;
        this.exception = null;
    }

    /**
     * Constructor.
     *
     * @param patchId     id patches which include sql
     * @param statement   sql command him self
     * @param processTime time in milliseconds of how long the application of the statement in DB took
     * @param finishedOn  date and time when the SQL statement was finished
     * @param exception   exception thrown during the statement application
     */
    public SqlCommand(int patchId, String statement, long processTime,
                      Date finishedOn, Exception exception) {
        validateSqlCommand(patchId, statement, processTime);
        this.patchId = patchId;
        this.statement = statement;
        this.processTime = processTime;
        this.finishedOn = finishedOn;
        this.exception = exception;
    }

    private void validateSqlCommand(int patchId, String statement, long processTime) throws SqlCommandFormatException {
        if (patchId <= 0) {
            throw new SqlCommandFormatException("PatchID can not be 0 or lower! patchId: " +
                    patchId + " statement: " + statement);
        }
        if (statement.isEmpty()) {
            throw new SqlCommandFormatException("Sql statement can not be empty! patchId: " +
                    patchId + " statement: " + statement);
        }
        if (processTime < 0) {
            throw new SqlCommandFormatException("ProcessTime can not be lower then 0! patchId: " +
                    patchId + " statement: " + statement);
        }


    }

}
