package com.novelosoftware.expenses.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
