package com.novelosoftware.expenses.services;

import com.novelosoftware.expenses.dto.*;
import com.novelosoftware.expenses.exceptions.AccountServiceExceptions;
import com.novelosoftware.expenses.mappers.AccountMapper;
import com.novelosoftware.expenses.repositories.AccountRepository;

import ch.qos.logback.core.util.StringUtil;

import org.springframework.stereotype.Service;


/**
 * Handles business logic for account operations.
 * Delegates persistence to AccountRepository and delegates mapping to AccountMapper.
 */
@Service
public class AccountService {

    private final AccountRepository repo;

    /**
     * @param repo   the repository for account persistence
     * @param mapper the mapper for converting between entities and DTOs
     */
    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    /**
     * Returns a single account by ID.
     *
     * @param id the account ID
     * @return the account DTO
     * @throws AccountNotFoundException if no account exists with the given ID
     */
    public Account getById(Long id) {
        return repo.findById(id)
            .map(AccountMapper::toDto)
            .orElseThrow(() -> AccountServiceExceptions.createAccountNotFoundException(id));
    }

    /**
     * Returns a paginated list of accounts belonging to a given user.
     *
     * @param userId the user ID to filter by
     * @param page   zero-based page number
     * @param size   number of items per page
     * @return paginated response containing account DTOs and pagination metadata
     */
    public PageResponse<Account> findByUser(String userId, int page, int size) {
        var total = repo.countByUser(userId);
        var content = repo.findByUser(userId, size, page * size)
            .stream().map(AccountMapper::toDto).toList();
        return PageResponse.of(content, page, size, total);
    }

    /**
     * Creates a new account.
     *
     * @param request the create request
     * @return response wrapping the created account
     */
    public CreateAccountResponse create(CreateAccountRequest request) {
        Account account = request.value();
        validateAccountName(account);

        var entity = AccountMapper.toEntity(request);
        return new CreateAccountResponse(AccountMapper.toDto(repo.create(entity)));
    }

    /**
     * Updates an existing account.
     *
     * @param accountId      the ID of the account to update
     * @param request the update request
     * @return response wrapping the updated account
     * @throws AccountNotFoundException if no account exists with the given ID
     */
    public UpdateAccountResponse update(Long accountId, UpdateAccountRequest request) {
        validateAccountName(request.value());
        var existing = repo.findById(accountId)
            .orElseThrow(() -> AccountServiceExceptions.createAccountNotFoundException(accountId));

        Account requested = request.value();
        var entity = existing.toBuilder()
            .name(requested.name())
            .accountType(requested.accountType())
            .currency(requested.currency())
            .initialAmount(requested.initialAmount() != null ? requested.initialAmount() : existing.initialAmount())
            .currentAmount(requested.currentAmount() != null ? requested.currentAmount() : existing.currentAmount())
            .build();
        return repo.update(accountId, entity)
            .map(AccountMapper::toDto)
            .map(UpdateAccountResponse::new)
            .orElseThrow(() -> AccountServiceExceptions.createAccountNotFoundException(accountId));
    }

    /**
     * Deletes an account by ID.
     *
     * @param accountId the ID of the account to delete
     * @throws AccountNotFoundException if no account exists with the given ID
     */
    public void delete(Long accountId) {
        if (!repo.delete(accountId)) {
            throw AccountServiceExceptions.createAccountNotFoundException(accountId);
        }
    }

    public void validateAccountName(Account account) {
        if (account == null) {
            throw AccountServiceExceptions.createValidationException("Request body must include a 'value' wrapper");
        }
        if (StringUtil.isNullOrEmpty(account.name())) {
            throw AccountServiceExceptions.createValidationException("Account name cannot be empty");
        }

        if (account.accountType() == null) {
            throw AccountServiceExceptions.createValidationException("Account type cannot be empty");
        }
    }
}
