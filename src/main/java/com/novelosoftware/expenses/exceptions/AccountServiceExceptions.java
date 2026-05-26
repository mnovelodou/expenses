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

    /** Creates an AccountValidationException for the given validation message */
    public static AccountValidationException createValidationException(String message) {
        return new AccountValidationException(message);
    }
    
    /**
     * Exception thrown when an account with a specified ID is not found in the database.
     */
    public static class AccountNotFoundException extends RuntimeException {

        private AccountNotFoundException(Long accountId) {
            super("Account with ID " + accountId + " not found.");
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
