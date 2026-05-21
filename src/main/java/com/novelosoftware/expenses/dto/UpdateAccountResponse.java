package com.novelosoftware.expenses.dto;

/**
 * Response body returned after successfully updating an account.
 * Wraps the updated account in a value field to allow adding response metadata in the future.
 */
public record UpdateAccountResponse(
    /** The updated account. */
    Account value
) {}
