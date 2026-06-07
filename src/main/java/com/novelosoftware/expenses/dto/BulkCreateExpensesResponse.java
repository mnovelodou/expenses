package com.novelosoftware.expenses.dto;

import java.util.List;

public record BulkCreateExpensesResponse(
    List<CreateExpenseResponse> expenses
) {}
