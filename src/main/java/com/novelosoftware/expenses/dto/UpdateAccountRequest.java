package com.novelosoftware.expenses.dto;

/**
 * Request body for updating an existing account.
 */
public record UpdateAccountRequest(
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

        public UpdateAccountRequest build() {
            return new UpdateAccountRequest(value);
        }
    }
}
