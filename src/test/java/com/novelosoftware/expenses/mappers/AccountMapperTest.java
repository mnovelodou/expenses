package com.novelosoftware.expenses.mappers;

import com.novelosoftware.expenses.dto.Account;
import com.novelosoftware.expenses.dto.AccountType;
import com.novelosoftware.expenses.dto.CreateAccountRequest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountMapperTest {

    private static CreateAccountRequest requestWith(BigDecimal initial, BigDecimal current) {
        var account = Account.builder()
            .name("Test")
            .accountType(AccountType.DEBIT)
            .currency("USD")
            .initialAmount(initial)
            .currentAmount(current)
            .createdBy("user-1")
            .build();
        return new CreateAccountRequest(account);
    }

    @Test
    void toEntity_usesSuppliedCurrentAmount() {
        var entity = AccountMapper.toEntity(requestWith(new BigDecimal("100"), new BigDecimal("250")));
        assertEquals(new BigDecimal("100"), entity.initialAmount());
        assertEquals(new BigDecimal("250"), entity.currentAmount());
    }

    @Test
    void toEntity_defaultsCurrentAmountToInitialWhenNull() {
        var entity = AccountMapper.toEntity(requestWith(new BigDecimal("100"), null));
        assertEquals(new BigDecimal("100"), entity.initialAmount());
        assertEquals(new BigDecimal("100"), entity.currentAmount());
    }

    @Test
    void toEntity_acceptsZeroCurrentAmount() {
        var entity = AccountMapper.toEntity(requestWith(new BigDecimal("500"), BigDecimal.ZERO));
        assertEquals(new BigDecimal("500"), entity.initialAmount());
        assertEquals(BigDecimal.ZERO, entity.currentAmount());
    }
}
