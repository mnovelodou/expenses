package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.dto.*;
import com.novelosoftware.expenses.services.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
     * Returns a paginated list of accounts belonging to a given user.
     *
     * @param userId the user ID
     * @param page   zero-based page number (default 0)
     * @param size   number of items per page (default 20)
     * @return paginated accounts owned by the user
     */
    @GetMapping("/user/{userId}")
    public PageResponse<Account> findByUser(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findByUser(userId, page, size);
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
        return service.create(request);
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
