package com.novelosoftware.expenses.dto;

/**
 * Response body returned after successfully creating an account.
 * Wraps the created account in a value field to allow adding response metadata in the future.
 */
public record CreateAccountResponse(
    /** The newly created account. */
    Account value
) {}
