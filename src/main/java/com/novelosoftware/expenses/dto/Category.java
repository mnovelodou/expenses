package com.novelosoftware.expenses.dto;

/**
 * Top-level budget category (Presupuesto) that groups one or more subcategories.
 */
public enum Category {
    /** Miscellaneous expenses that don't fit elsewhere. */
    MISCELLANEOUS,
    /** Monthly rent payment. */
    RENT,
    /** Grocery and supermarket shopping. */
    GROCERIES,
    /** Expenses related to spouse. */
    SPOUSE,
    /** All vehicle-related expenses. */
    CAR,
    /** Food and dining expenses. */
    FOOD,
    /** Recurring utility and service bills. */
    SERVICES,
    /** Debt repayments. */
    DEBT,
    /** General day-to-day spending. */
    GENERAL,
    /** Monthly car loan or lease payment. */
    CAR_MONTHLY_PAYMENT,
    /** International wire transfers. */
    INTERNATIONAL_TRANSFER,
    /** Income received. */
    INCOME
}
