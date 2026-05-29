package com.novelosoftware.expenses.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.novelosoftware.expenses.exceptions.AccountServiceExceptions.AccountNotFoundException;
import com.novelosoftware.expenses.exceptions.AccountServiceExceptions.AccountValidationException;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.ExpenseValidationException;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.UnauthorizedAccountException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Global exception handler for all REST controllers.
 * Intercepts exceptions thrown during request processing and returns
 * a consistent ErrorResponse shape with an appropriate HTTP status.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles requests for accounts that do not exist.
     *
     * @param ex the exception carrying the missing account ID
     * @return 404 response with NOT_FOUND error code
     */
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    /**
     * Handles malformed JSON and invalid enum values in the request body.
     * Produces a descriptive message listing accepted values when an enum field is invalid.
     *
     * @param ex the exception thrown during deserialization
     * @return 400 response with BAD_REQUEST error code
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        String message = "Malformed request body";
        if (ex.getCause() instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
            String accepted = Arrays.stream(ife.getTargetType().getEnumConstants())
                .map(Object::toString)
                .collect(Collectors.joining(", "));
            message = "Invalid value '" + ife.getValue() + "'. Accepted values: " + accepted;
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("BAD_REQUEST", message));
    }

    /**
     * Handles FK constraint violations, e.g. deleting an account that still has expenses.
     *
     * @param ex the data integrity violation
     * @return 412 response with PRECONDITION_FAILED code
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
            .body(new ErrorResponse("PRECONDITION_FAILED", "Cannot complete operation due to existing references"));
    }

    /**
     * Handles unexpected JDBC/database errors.
     *
     * @param ex the Spring DataAccessException
     * @return 500 response with DATABASE_ERROR code, details are logged server-side
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabase(DataAccessException ex) {
        log.error("Database error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("DATABASE_ERROR", "An unexpected database error occurred"));
    }

    /**
     * Handles invalid accounts on write operations
     * @param ex the exception carrying the invalid reason
     * @return 400 response with BAD_REQUEST error code
     */
    @ExceptionHandler(AccountValidationException.class)
    public ResponseEntity<ErrorResponse> handleAccountValidationException(AccountValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    /**
     * Handles invalid accounts on write operations
     * @param ex the exception carrying the invalid reason
     * @return 400 response with BAD_REQUEST error code
     */
    @ExceptionHandler(ExpenseValidationException.class)
    public ResponseEntity<ErrorResponse> handleExpenseValidationException(ExpenseValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("BAD_REQUEST", ex.getMessage()));
    }

    /**
     * Handles unauthorized accounts on expense writing
     * @param ex the exception carrying the invalid reason
     * @return 403 response with UNAUTHORIZED error code
     */
    @ExceptionHandler(UnauthorizedAccountException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccountException(UnauthorizedAccountException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse("FORBIDDEN", ex.getMessage()));
    }

    /**
     * Catch-all handler for any unhandled exceptions.
     *
     * @param ex the unhandled exception
     * @return 500 response with INTERNAL_ERROR code, details are logged server-side
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    /**
     * Standard error response body returned for all handled exceptions.
     *
     * @param code    a machine-readable error code (e.g. NOT_FOUND, DATABASE_ERROR)
     * @param message a human-readable description of the error
     */
    public record ErrorResponse(String code, String message) {}
}
