package com.novelosoftware.expenses.services;

import com.novelosoftware.expenses.dto.AccountRequest;
import com.novelosoftware.expenses.dto.AccountResponse;
import com.novelosoftware.expenses.entities.AccountEntity;
import com.novelosoftware.expenses.exceptions.AccountNotFoundException;
import com.novelosoftware.expenses.repositories.AccountRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository repo;

    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    public List<AccountResponse> getAll() {
        return repo.findAll().stream().map(this::toResponse).toList();
    }

    public AccountResponse getById(Long id) {
        return repo.findById(id)
            .map(this::toResponse)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public List<AccountResponse> getByUser(String userId) {
        return repo.findByUser(userId).stream().map(this::toResponse).toList();
    }

    public AccountResponse create(AccountRequest request, String userId) {
        var entity = new AccountEntity(null, request.name(), request.accountType(),
            request.currency(), request.initialAmount(), request.initialAmount(),
            null, null, userId);
        return toResponse(repo.create(entity));
    }

    public AccountResponse update(Long id, AccountRequest request) {
        repo.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        var entity = new AccountEntity(null, request.name(), request.accountType(),
            request.currency(), request.initialAmount(), request.initialAmount(),
            null, null, null);
        return repo.update(id, entity)
            .map(this::toResponse)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public void delete(Long id) {
        if (!repo.delete(id)) throw new AccountNotFoundException(id);
    }

    private AccountResponse toResponse(AccountEntity e) {
        return new AccountResponse(e.accountId(), e.name(), e.accountType(),
            e.currency(), e.currentAmount());
    }
}
