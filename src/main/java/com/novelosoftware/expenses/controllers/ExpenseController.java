package com.novelosoftware.expenses.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.novelosoftware.expenses.dto.CreateExpenseRequest;
import com.novelosoftware.expenses.dto.CreateExpenseResponse;
import com.novelosoftware.expenses.services.ExpenseService;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {
    
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
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
}
