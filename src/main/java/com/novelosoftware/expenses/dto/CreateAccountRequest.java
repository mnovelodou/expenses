package com.novelosoftware.expenses.dto;

/**
 * Request body for creating a new account.
 */
public record CreateAccountRequest(
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

        public CreateAccountRequest build() {
            return new CreateAccountRequest(value);
        }
    }
}
