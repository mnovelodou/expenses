package com.novelosoftware.expenses.dto;

import java.util.List;

public record BulkCreateExpensesRequest(
    List<CreateExpenseRequest> expenses
) {}
