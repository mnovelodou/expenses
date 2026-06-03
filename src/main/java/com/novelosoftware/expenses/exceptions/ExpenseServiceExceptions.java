package com.novelosoftware.expenses.exceptions;

import com.novelosoftware.expenses.dto.Expense;

/**
 * Exception factory for Expenses service
 */
public final class ExpenseServiceExceptions {
    
    private ExpenseServiceExceptions() {}

    /**
     * Creates an ExpenseValidationException
     * @param message description of the exception
     * @return an ExpenseValidationException
     */
    public static ExpenseValidationException createValidationException(String message) {
        return new ExpenseValidationException(message);
    }

    /**
     * Creates an UnauthorizedAccountException
     * @param message description of the exception
     * @return an ExpenseValidationException
     */
    public static UnauthorizedExpenseException createUnauthorizedExpenseException(String message){
        return new UnauthorizedExpenseException(message);
    }

    /**
     * Creates an ExpenseNotFoundException
     * @param message description of the exception
     * @return an ExpenseNotFoundException
     */
    public static ExpenseNotFoundException createExpenseNotFoundException(Long id) {
        return new ExpenseNotFoundException("Expense id " + id + " not found");
    }

    /**
     * Exception used for invalid expenses.
     */
    public static final class ExpenseValidationException extends RuntimeException {
        private ExpenseValidationException(String message) {
            super(message);
        } 
    }

    /**
     * Unauthorized access to accounts that don't belong to the user.
     */
    public static final class UnauthorizedExpenseException extends RuntimeException {
        private UnauthorizedExpenseException(String message) {
            super(message);
        }
    }

    public static final class ExpenseNotFoundException extends RuntimeException {
        private ExpenseNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Creates an InvalidCursorException for a malformed cursor token.
     *
     * @return an InvalidCursorException
     */
    public static InvalidCursorException createInvalidCursorException(String message) {
        return new InvalidCursorException(message);
    }

    /**
     * Thrown when a cursor token is malformed or its encoded date falls outside
     * the requested date range.
     */
    public static final class InvalidCursorException extends RuntimeException {
        private InvalidCursorException(String message) {
            super(message);
        }
    }
}
