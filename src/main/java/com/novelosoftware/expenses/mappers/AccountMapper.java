package com.novelosoftware.expenses.mappers;

import com.novelosoftware.expenses.dto.Account;
import com.novelosoftware.expenses.dto.CreateAccountRequest;
import com.novelosoftware.expenses.dto.UpdateAccountRequest;
import com.novelosoftware.expenses.entities.AccountEntity;
import org.springframework.stereotype.Component;

/**
 * Maps between AccountEntity and Account DTOs.
 * Centralizes all translation logic between the persistence and API layers.
 */
@Component
public class AccountMapper {

    /**
     * Converts a persisted entity to an API-facing Account DTO.
     *
     * @param entity the account entity from the database
     * @return the corresponding Account DTO
     */
    public Account toDto(AccountEntity entity) {
        return new Account(
            entity.accountId(),
            entity.name(),
            entity.accountType(),
            entity.currency(),
            entity.initialAmount(),
            entity.currentAmount(),
            entity.createdBy()
        );
    }

    /**
     * Converts a create request into a new AccountEntity.
     * accountId and audit timestamps are left null to be assigned by the database.
     *
     * @param request the create request from the API
     * @param userId  the ID of the user creating the account
     * @return a new AccountEntity ready for insertion
     */
    public AccountEntity toEntity(CreateAccountRequest request, String userId) {
        return new AccountEntity(
            null,
            request.name(),
            request.accountType(),
            request.currency(),
            request.initialAmount(),
            request.initialAmount(),
            null,
            null,
            userId
        );
    }

    /**
     * Converts an update request into an AccountEntity for use in update queries.
     * accountId, initialAmount, and audit timestamps are left null as they are managed elsewhere.
     *
     * @param request the update request from the API
     * @return an AccountEntity carrying the updated fields
     */
    public AccountEntity toEntity(UpdateAccountRequest request) {
        return new AccountEntity(
            null,
            request.name(),
            request.accountType(),
            request.currency(),
            null,
            request.currentAmount(),
            null,
            null,
            null
        );
    }
}
