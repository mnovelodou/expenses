package com.novelosoftware.expenses.dto;

/**
 * Request for creating a new expense.
 */
public record CreateExpenseRequest(
    Expense value
) {}
