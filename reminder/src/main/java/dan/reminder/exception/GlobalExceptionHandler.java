/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.exception;

import dan.reminder.exception.dto.ApiErrorResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.validation.method.ParameterErrors;

import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;

/**
 *
 * @author danil
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

//    MethodArgumentNotValidException - @Valid @RequestBody
//    InvalidPageRequestException - некорректные параметры пагинации. Не срабатывает, потому что Spring Data автоматически 
//                                заменяет невалидные параметры на дефолтные значения
//    InvalidDateRangeException - некорректный диапазон дат
//    HandlerMethodValidationException - RequestParam("title") @NotBlank(message = "Title must not be empty") String title
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse<List<String>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        return buildValidationErrorResponse(
                ex.getBindingResult().getFieldErrors()
                        .stream()
                        .collect(Collectors.toMap(
                                err -> err.getField(),
                                err -> List.of(err.getDefaultMessage()),
                                (list1, list2) -> { // если несколько ошибок на поле
                                    List<String> merged = new ArrayList<>(list1);
                                    merged.addAll(list2);
                                    return merged;
                                }
                        ))
        );
    }

    private ResponseEntity<ApiErrorResponse<List<String>>> buildValidationErrorResponse(Map<String, List<String>> details) {
        ApiErrorResponse<List<String>> error = new ApiErrorResponse<>(
                "VALIDATION_ERROR",
                "Invalid input data",
                details
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                "INTERNAL_SERVER_ERROR",
                "Unexpected error occurred",
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(InvalidPageRequestException.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleInvalidPageRequest(InvalidPageRequestException ex) {
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                ex.getErrorCode(),
                ex.getMessage(),
                Map.of("page", ex.getPage(), "size", ex.getSize())
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                ex.getErrorCode(),
                ex.getMessage(),
                Map.of("start", ex.getStart(), "end", ex.getEnd())
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidTimezoneException.class)
    public ResponseEntity<ApiErrorResponse<String>> handleInvalidTimezone(InvalidTimezoneException ex) {
        ApiErrorResponse<String> error = new ApiErrorResponse<>(
                "INVALID_TIMEZONE",
                ex.getMessage(),
                Map.of("X-Timezone", ex.getTimezone() == null ? "" : ex.getTimezone())
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(InvalidReminderTimeException.class)
    public ResponseEntity<ApiErrorResponse<String>> handleInvalidReminderTime(InvalidReminderTimeException ex) {
        ApiErrorResponse<String> error = new ApiErrorResponse<>(
                "INVALID_REMINDER_TIME",
                ex.getMessage(),
                Map.of(
                        "remind", ex.getLocalDateTime().toString(),
                        "timezone", ex.getTimezone().getId())
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiErrorResponse<List<String>>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, List<String>> details = new HashMap<>();

        ex.getAllValidationResults().forEach(result -> {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors().forEach(fieldError ->
                        details.computeIfAbsent(fieldError.getField(), k -> new ArrayList<>())
                                .add(fieldError.getDefaultMessage()));
            } else {
                String parameter = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error ->
                        details.computeIfAbsent(parameter, k -> new ArrayList<>())
                                .add(error.getDefaultMessage()));
            }
        });

        ApiErrorResponse<List<String>> error = new ApiErrorResponse<>(
                "VALIDATION_ERROR",
                "Invalid input data",
                details
        );

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse<String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> details = Map.of(
                ex.getName(), "Invalid value: " + ex.getValue()
        );
        ApiErrorResponse<String> error = new ApiErrorResponse<>(
                "INVALID_PARAMETER",
                "Invalid parameter: " + ex.getName(),
                details
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse<String>> handleMalformedBody(HttpMessageNotReadableException ex) {
        ApiErrorResponse<String> error = new ApiErrorResponse<>(
                "MALFORMED_REQUEST",
                "Request body is malformed or contains invalid values",
                Map.of("body", "Valid JSON is required")
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse<String>> handleMissingRequestParameter(
            MissingServletRequestParameterException ex) {
        ApiErrorResponse<String> error = new ApiErrorResponse<>(
                "MISSING_REQUEST_PARAMETER",
                "Required request parameter is missing",
                Map.of(ex.getParameterName(), "Parameter is required")
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ApiErrorResponse<String>> handleInvalidSort(PropertyReferenceException ex) {
        ApiErrorResponse<String> error = new ApiErrorResponse<>(
                "INVALID_SORT",
                "Unsupported sort property",
                Map.of("sort", ex.getPropertyName())
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse<String>> handleMissingRequestHeader(
            MissingRequestHeaderException ex) {

        ApiErrorResponse<String> error = new ApiErrorResponse<>(
                "MISSING_REQUEST_HEADER",
                "Required request header is missing",
                Map.of(ex.getHeaderName(), "Header is required")
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ReminderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleReminderNotFound(ReminderNotFoundException ex) {
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                "REMINDER_NOT_FOUND",
                "Reminder not found",
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IdempotencyInProgressException.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleIdempotencyInProgress(IdempotencyInProgressException ex) {
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                "IDEMPOTENCY_IN_PROGRESS",
                "Request is already being processed",
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IdempotencyRecordNotFoundException.class)
    public ResponseEntity<ApiErrorResponse<Object>> handleIdempotencyRecordNotFound(
            IdempotencyRecordNotFoundException ex) {
        log.error("Idempotency record refers to a missing reminder", ex);
        ApiErrorResponse<Object> error = new ApiErrorResponse<>(
                "INTERNAL_SERVER_ERROR",
                "Unexpected error occurred",
                Map.of()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

}
