package com.novelosoftware.expenses.mappers;

import com.novelosoftware.expenses.dto.Account;
import com.novelosoftware.expenses.dto.CreateAccountRequest;
import com.novelosoftware.expenses.dto.UpdateAccountRequest;
import com.novelosoftware.expenses.entities.AccountEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.time.LocalDate;

/**
 * Maps between AccountEntity and Account DTOs.
 * Centralizes all translation logic between the persistence and API layers.
 */
public final class AccountMapper {

    private AccountMapper() {}


    /**
     * Converts a persisted entity to an API-facing Account DTO without a gap value.
     *
     * @param entity the account entity from the database
     * @return the corresponding Account DTO with a null gap
     */
    public static Account toDto(AccountEntity entity) {
        return toDto(entity, null);
    }

    /**
     * Converts a persisted entity to an API-facing Account DTO.
     *
     * @param entity the account entity from the database
     * @param gap    the reconciliation gap to attach, or null when not requested
     * @return the corresponding Account DTO
     */
    public static Account toDto(AccountEntity entity, BigDecimal gap) {
        return new Account(
            entity.accountId(),
            entity.name(),
            entity.accountType(),
            entity.currency(),
            entity.initialAmount(),
            entity.currentAmount(),
            entity.createdBy(),
            entity.periodStart(),
            gap
        );
    }

    /**
     * Converts a create request into a new AccountEntity.
     * accountId and audit timestamps are left null to be assigned by the database.
     *
     * @param account the create request from the API
     * @param userId  the ID of the user creating the account
     * @return a new AccountEntity ready for insertion
     */
    public static AccountEntity toEntity(CreateAccountRequest request) {
        Account account = request.value();
        BigDecimal initial = Optional.ofNullable(account.initialAmount()).orElse(BigDecimal.ZERO);
        BigDecimal current = Optional.ofNullable(account.currentAmount()).orElse(initial);
        return new AccountEntity(
            null,
            account.name(),
            account.accountType(),
            account.currency(),
            initial,
            current,
            OffsetDateTime.now(),
            OffsetDateTime.now(),
            account.createdBy(),
            account.periodStart()
        );
    }

    /**
     * Converts an update request into an AccountEntity for use in update queries.
     * accountId, initialAmount, and audit timestamps are left null as they are managed elsewhere.
     *
     * @param request the update request from the API
     * @return an AccountEntity carrying the updated fields
     */
    public static AccountEntity toEntity(UpdateAccountRequest request) {
        return toEntity(request.value());
    }

    /**
     * Converts a create request into a new AccountEntity.
     * accountId and audit timestamps are left null to be assigned by the database.
     * @param account the create request from the API
     * @param userId the ID of the user creating the account
     * @return
     */
    private static AccountEntity toEntity(Account account) {
        return new AccountEntity(
            account.accountId(),
            account.name(),
            account.accountType(),
            account.currency(),
            account.initialAmount(),
            account.currentAmount(),
            null,
            null,
            account.createdBy(),
            account.periodStart()
        );
    }
}
