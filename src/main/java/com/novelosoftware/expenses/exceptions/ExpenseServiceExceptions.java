package com.novelosoftware.expenses.exceptions;

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
    public static UnauthorizedAccountException createUnauthorizedAccountException(String message){
        return new UnauthorizedAccountException(message);
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
    public static final class UnauthorizedAccountException extends RuntimeException {
        private UnauthorizedAccountException(String message) {
            super(message);
        }
    }
}
