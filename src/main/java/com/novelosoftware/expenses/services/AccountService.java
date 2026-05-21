package com.novelosoftware.expenses.services;

import com.novelosoftware.expenses.dto.*;
import com.novelosoftware.expenses.exceptions.AccountNotFoundException;
import com.novelosoftware.expenses.mappers.AccountMapper;
import com.novelosoftware.expenses.repositories.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles business logic for account operations.
 * Delegates persistence to AccountRepository and delegates mapping to AccountMapper.
 */
@Service
public class AccountService {

    private final AccountRepository repo;
    private final AccountMapper mapper;

    /**
     * @param repo   the repository for account persistence
     * @param mapper the mapper for converting between entities and DTOs
     */
    public AccountService(AccountRepository repo, AccountMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    /**
     * Returns all accounts.
     *
     * @return list of all accounts as DTOs
     */
    public List<Account> getAll() {
        return repo.findAll().stream().map(mapper::toDto).toList();
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
            .map(mapper::toDto)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }

    /**
     * Returns all accounts belonging to a given user.
     *
     * @param userId the user ID to filter by
     * @return list of account DTOs owned by the user
     */
    public List<Account> getByUser(String userId) {
        return repo.findByUser(userId).stream().map(mapper::toDto).toList();
    }

    /**
     * Creates a new account.
     *
     * @param request the create request
     * @param userId  the ID of the user creating the account
     * @return response wrapping the created account
     */
    public CreateAccountResponse create(CreateAccountRequest request, String userId) {
        var entity = mapper.toEntity(request, userId);
        return new CreateAccountResponse(mapper.toDto(repo.create(entity)));
    }

    /**
     * Updates an existing account.
     *
     * @param id      the ID of the account to update
     * @param request the update request
     * @return response wrapping the updated account
     * @throws AccountNotFoundException if no account exists with the given ID
     */
    public UpdateAccountResponse update(Long id, UpdateAccountRequest request) {
        repo.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        var entity = mapper.toEntity(request);
        return repo.update(id, entity)
            .map(mapper::toDto)
            .map(UpdateAccountResponse::new)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }

    /**
     * Deletes an account by ID.
     *
     * @param id the ID of the account to delete
     * @throws AccountNotFoundException if no account exists with the given ID
     */
    public void delete(Long id) {
        if (!repo.delete(id)) throw new AccountNotFoundException(id);
    }
}
