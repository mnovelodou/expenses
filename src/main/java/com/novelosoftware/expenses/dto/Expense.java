package com.novelosoftware.expenses.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Expense exposes a single expense line
 */
public record Expense (
    /** ID of the expense */
    Long expenseId,
    /** Date when the expense happened */
    LocalDate expenseDate,
    /** refernece to the account when expense was originated */
    Long accountId,
    /** amount of the expense */
    BigDecimal amount,
    /** description of the expense */
    String description,
    /** sub-category o the expense, category can be infered from here */
    SubCategory subCategory,
    /** onwer of expense */
    String createdBy 
){}
