package com.novelosoftware.expenses.dto;

import java.math.BigDecimal;

public record AccountResponse(
    Long id,
    String name,
    String accountType,
    String currency,
    BigDecimal currentAmount
) {}
