package com.novelosoftware.expenses.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.novelosoftware.expenses.dto.BulkCreateExpensesRequest;
import com.novelosoftware.expenses.dto.BulkCreateExpensesResponse;
import com.novelosoftware.expenses.dto.CreateExpenseRequest;
import com.novelosoftware.expenses.dto.CreateExpenseResponse;
import com.novelosoftware.expenses.dto.CursorPageResponse;
import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.UpdateExpenseRequest;
import com.novelosoftware.expenses.dto.UpdateExpenseResponse;
import com.novelosoftware.expenses.services.ExpenseService;

import java.time.LocalDate;

/**
 * ExpenseController contains APIs for Expense entities.
 */
@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /**
     * Lists expenses for a user with forward cursor pagination and optional filters.
     *
     * <p>{@code category} and {@code subcategory} are mutually exclusive; supplying both
     * results in HTTP 400. {@code account_id} may be combined with either.
     *
     * @param userId      required; the user whose expenses to list
     * @param startDate   optional start of date window; defaults to first day of last month
     * @param endDate     optional end of date window; defaults to last day of last month
     * @param limit       optional page size (1–100); defaults to 20
     * @param cursor      optional opaque cursor from a previous response's {@code nextCursor}
     * @param category    optional category filter; mutually exclusive with subcategory
     * @param subcategory optional subcategory filter; mutually exclusive with category
     * @param accountId   optional account filter
     * @return a page of expenses and an optional next-page cursor
     */
    @GetMapping
    public CursorPageResponse<Expense> list(
            @RequestParam(value = "user_id", required = false) String userId,
            @RequestParam(value = "start_date", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "end_date", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "subcategory", required = false) String subcategory,
            @RequestParam(value = "account_id", required = false) Long accountId) {

        return expenseService.listByUser(userId, startDate, endDate, limit, cursor, category, subcategory, accountId);
    }

    /**
     * Creates an expense
     * @param request wrapper with an Expense
     * @return a wrapper with the created Expense
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateExpenseResponse create(@RequestBody CreateExpenseRequest request) {
        return expenseService.create(request);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkCreateExpensesResponse bulkCreate(@RequestBody BulkCreateExpensesRequest request) {
        return new BulkCreateExpensesResponse(expenseService.bulkCreate(request.expenses()));
    }

    @GetMapping("/{id}")
    public Expense getById(@PathVariable Long id) {
        return expenseService.getById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        expenseService.delete(id);
    }

    @PutMapping("/{id}")
    public UpdateExpenseResponse update(@PathVariable Long id, @RequestBody UpdateExpenseRequest request) {
        return expenseService.update(id, request);
    }
}
