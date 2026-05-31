package com.novelosoftware.expenses.dto;


public record UpdateExpenseRequest(
    Expense value
) {
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder().value(value);
    }

    public static final class Builder {
        private Expense value;

        private Builder() {}

        public Builder value(Expense value) { this.value = value; return this; }

        public UpdateExpenseRequest build() {
            return new UpdateExpenseRequest(value);
        }
    }
}
