package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.dto.*;
import com.novelosoftware.expenses.services.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for account operations.
 * Delegates all business logic to AccountService.
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService service;

    /**
     * @param service the service handling account business logic
     */
    public AccountController(AccountService service) {
        this.service = service;
    }

    /**
     * Returns all accounts.
     *
     * @return list of all accounts
     */
    @GetMapping
    public List<Account> getAll() {
        return service.getAll();
    }

    /**
     * Returns a single account by ID.
     *
     * @param id the account ID
     * @return the account
     */
    @GetMapping("/{id}")
    public Account getById(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * Returns all accounts belonging to a given user.
     *
     * @param userId the user ID
     * @return list of accounts owned by the user
     */
    @GetMapping("/user/{userId}")
    public List<Account> getByUser(@PathVariable String userId) {
        return service.getByUser(userId);
    }

    /**
     * Creates a new account.
     *
     * @param request the create request body
     * @return the created account wrapped in a response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAccountResponse create(@RequestBody CreateAccountRequest request) {
        return service.create(request, "default_user_id");
    }

    /**
     * Updates an existing account.
     *
     * @param id      the ID of the account to update
     * @param request the update request body
     * @return the updated account wrapped in a response
     */
    @PutMapping("/{id}")
    public UpdateAccountResponse update(@PathVariable Long id, @RequestBody UpdateAccountRequest request) {
        return service.update(id, request);
    }

    /**
     * Deletes an account by ID.
     *
     * @param id the ID of the account to delete
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
