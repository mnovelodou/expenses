package com.novelosoftware.expenses.services;

import org.springframework.stereotype.Service;

import com.novelosoftware.expenses.dto.Account;
import com.novelosoftware.expenses.dto.CreateExpenseRequest;
import com.novelosoftware.expenses.dto.CreateExpenseResponse;
import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.mappers.ExpenseMapper;
import com.novelosoftware.expenses.repositories.ExpenseRepository;

import static com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.*;

@Service
public class ExpenseService {
    
    private final ExpenseRepository repo;
    private final AccountService accountService;

    public ExpenseService(ExpenseRepository repo, AccountService accountService) {
        this.repo = repo;
        this.accountService = accountService;
    }

    public CreateExpenseResponse create(CreateExpenseRequest request) {
        Expense expense = request.value();
        expenseWriteValidations(expense);

        ExpenseEntity expenseEntity = ExpenseMapper.toEntity(expense);
        ExpenseEntity createdEntity = repo.create(expenseEntity);
        return new CreateExpenseResponse(ExpenseMapper.toDto(createdEntity));
    }  

    private void expenseWriteValidations(Expense expense) {
        if (expense.expenseDate() == null) {
            throw createValidationException("expenseDate cannot be null");
        }

        if (expense.accountId() == null) {
            throw createValidationException("accountId cannot be null");
        }

        if (expense.amount() == null) {
            throw createValidationException("amount cannot be null");
        }

        if (expense.description() == null || expense.description().isEmpty()) {
            throw createValidationException("description cannot be null");
        }

        if (expense.subCategory() == null) {
            throw createValidationException("subcategory cannot be null");
        }

        if (expense.createdBy() == null || expense.createdBy().isEmpty()) {
            throw createValidationException("createdBy cannot be null");
        }

        Account account = accountService.getById(expense.accountId());
        
        if (!account.createdBy().equals(expense.createdBy())) {
            throw createUnauthorizedAccountException("User does not own the given account");
        }
    }
}
