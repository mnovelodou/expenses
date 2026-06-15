package com.novelosoftware.expenses.exceptions;

/**
 * Custom exceptions for the AccountService.
 */
public final class AccountServiceExceptions {

    private AccountServiceExceptions() {
        // Private constructor to prevent instantiation
    }

    /** creates an AccountNotFoundException for the given account ID */
    public static AccountNotFoundException createAccountNotFoundException(Long accountId) {
        return new AccountNotFoundException(accountId);
    }

    /** creates an AccountNotFoundException with a custom message, used to hide ownership */
    public static AccountNotFoundException createAccountNotFoundException(String message) {
        return new AccountNotFoundException(message);
    }

    /** Creates an AccountValidationException for the given validation message */
    public static AccountValidationException createValidationException(String message) {
        return new AccountValidationException(message);
    }

    /** Creates an UnauthorizedAccountException for the given reason */
    public static UnauthorizedAccountException createUnauthorizedAccountException(String message) {
        return new UnauthorizedAccountException(message);
    }

    /**
     * Exception thrown when an account with a specified ID is not found in the database.
     */
    public static class AccountNotFoundException extends RuntimeException {

        private AccountNotFoundException(Long accountId) {
            super("Account with ID " + accountId + " not found.");
        }

        private AccountNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when a caller attempts to act on behalf of another user.
     */
    public static class UnauthorizedAccountException extends RuntimeException {
        private UnauthorizedAccountException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when an account fails validation.
     */
    public static class AccountValidationException extends RuntimeException {
        public AccountValidationException(String message) {
            super(message);
        }
    }
}
