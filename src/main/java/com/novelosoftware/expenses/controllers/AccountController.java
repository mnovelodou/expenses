package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.dto.*;
import com.novelosoftware.expenses.services.AccountService;
import com.novelosoftware.expenses.services.ExpenseService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for account operations.
 * Delegates all business logic to AccountService.
 */
@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService service;
    private final ExpenseService expenseService;

    /**
     * @param service        the service handling account business logic
     * @param expenseService the service handling expense queries
     */
    public AccountController(AccountService service, ExpenseService expenseService) {
        this.service = service;
        this.expenseService = expenseService;
    }

    /**
     * Returns a single account by ID.
     *
     * @param id the account ID
     * @return the account
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_read:accounts')")
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
    @PreAuthorize("hasAuthority('SCOPE_read:accounts')")
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
    @PreAuthorize("hasAuthority('SCOPE_write:accounts')")
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
    @PreAuthorize("hasAuthority('SCOPE_write:accounts')")
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
    @PreAuthorize("hasAuthority('SCOPE_write:accounts')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /**
     * Lists expenses for a specific account with cursor pagination and optional filters.
     * The owner (user_id) is resolved from the account, so callers do not need to supply it.
     *
     * @param id          the account ID (path)
     * @param startDate   optional start of date window; defaults to first day of last month
     * @param endDate     optional end of date window; defaults to last day of last month
     * @param limit       optional page size (1–100); defaults to 20
     * @param cursor      optional opaque cursor from a previous response's {@code nextCursor}
     * @param category    optional category filter; mutually exclusive with subcategory
     * @param subcategory optional subcategory filter; mutually exclusive with category
     * @return a page of expenses and an optional next-page cursor
     */
    @GetMapping("/{id}/expenses")
    @PreAuthorize("hasAuthority('SCOPE_read:expenses')")
    public CursorPageResponse<Expense> listExpenses(
            @PathVariable Long id,
            @RequestParam(value = "start_date", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subcategory", required = false) String subcategory) {

        String userId = service.getById(id).createdBy();
        return expenseService.listByUser(userId, startDate, endDate, limit, cursor, category, subcategory, id);
    }
}
