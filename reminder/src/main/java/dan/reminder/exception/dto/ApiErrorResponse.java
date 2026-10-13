/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.exception.dto;

import java.util.Map;

/**
 *
 * @author danil
 */

public class ApiErrorResponse<T> {
    private final String errorCode;
    private final String message;
    private final Map<String, T> details;

    public ApiErrorResponse(String errorCode, String message, Map<String, T> details) {
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public Map<String, T> getDetails() { return details; }
}
