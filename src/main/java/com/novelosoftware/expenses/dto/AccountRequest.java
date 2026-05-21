package com.novelosoftware.expenses.dto;

import java.math.BigDecimal;

public record AccountRequest(
    String name,
    String accountType,
    String currency,
    BigDecimal initialAmount
) {}
