package com.novelosoftware.expenses.dto;

/**
 * Response body returned after successfully updating an account.
 * Wraps the updated account in a value field to allow adding response metadata in the future.
 */
public record UpdateAccountResponse(
    /** The updated account. */
    Account value
) {
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().value(value);
    }

    public static final class Builder {
        private Account value;

        private Builder() {}

        public Builder value(Account value) { this.value = value; return this; }

        public UpdateAccountResponse build() {
            return new UpdateAccountResponse(value);
        }
    }
}
