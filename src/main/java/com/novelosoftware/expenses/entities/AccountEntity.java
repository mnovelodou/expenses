package com.novelosoftware.expenses.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountEntity(
    Long accountId,
    String name,
    String accountType,
    String currency,
    BigDecimal initialAmount,
    BigDecimal currentAmount,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    String createdBy
) {}
