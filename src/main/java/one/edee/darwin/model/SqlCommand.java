package one.edee.darwin.model;

import lombok.Data;
import one.edee.darwin.exception.SqlCommandFormatException;

import java.util.Date;

/**
 * This is class form working with sql statement in patch{@link Patch}.
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
     * @param patchId     id patches which include sql
     * @param statement   sql command him self
     */
    public SqlCommand(int patchId, String statement, long processTime, Date finishedOn) {
        sqlCommandValidation(patchId, statement, processTime, finishedOn);
        this.patchId = patchId;
        this.statement = statement;
        this.processTime = processTime;
        this.finishedOn = finishedOn;
        this.exception = null;
    }

    public SqlCommand(int patchId, String statement, long processTime,
                      Date finishedOn, Exception exception) {
        sqlCommandValidation(patchId, statement, processTime, finishedOn);
        this.patchId = patchId;
        this.statement = statement;
        this.processTime = processTime;
        this.finishedOn = finishedOn;
        this.exception = exception;
    }

    private void sqlCommandValidation(int patchId, String statement,
                                      long processTime, Date finishedOn) throws SqlCommandFormatException {
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
