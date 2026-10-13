/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.exception;

import java.time.Instant;

/**
 *
 * @author danil
 */
public class InvalidDateRangeException extends RuntimeException {
    private final String errorCode = "INVALID_DATE_RANGE";
    private final Instant start;
    private final Instant end;

    public InvalidDateRangeException(Instant start, Instant end) {
        super("Invalid date range: start is after end");
        this.start = start;
        this.end = end;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getStart() {
        return start;
    }

    public Instant getEnd() {
        return end;
    }
}


