package com.novelosoftware.expenses.entities;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Internal representation of an expense row in the database.
 * Not exposed directly to API consumers — use Expense DTO instead.
 */
public record ExpenseEntity(
    /** Auto-generated primary key. */
    Long expenseId,
    /** Date the expense occurred. */
    LocalDate expenseDate,
    /** ID of the account this expense belongs to. */
    Long accountId,
    /** Amount spent. */
    BigDecimal amount,
    /** Description of what the expense was for. */
    String description,
    /** Top-level category (e.g. Food, Transport). */
    String category,
    /** Sub-category within the category (e.g. Restaurants, Uber). */
    String subcategory,
    /** ID of the user who created this expense. */
    String createdBy
) {}
