package com.novelosoftware.expenses.exceptions;

/**
 * Thrown when an account with the requested ID does not exist.
 * Handled by GlobalExceptionHandler, which maps it to a 404 response.
 */
public class AccountNotFoundException extends RuntimeException {

    /**
     * @param id the ID of the account that was not found
     */
    public AccountNotFoundException(Long id) {
        super("Account not found: " + id);
    }
}
