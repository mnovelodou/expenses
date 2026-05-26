package com.novelosoftware.expenses.dto;

/**
 * Create expense reponse wrapper with created expense.
 */
public record CreateExpenseResponse(
    /** The newly created expense */
    Expense value
) {}
