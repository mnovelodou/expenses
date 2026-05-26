package com.novelosoftware.expenses.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novelosoftware.expenses.dto.Account;
import com.novelosoftware.expenses.dto.AccountType;
import com.novelosoftware.expenses.dto.CreateExpenseRequest;
import com.novelosoftware.expenses.dto.CreateExpenseResponse;
import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.SubCategory;
import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.ExpenseValidationException;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.UnauthorizedAccountException;
import com.novelosoftware.expenses.mappers.CategoryMapper;
import com.novelosoftware.expenses.repositories.ExpenseRepository;

/**
 * Test class for {@link ExpenseService.class}
 */
@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {
    
    private static final Expense VALID_NEW_EXPENSE = new Expense(
        null, 
        LocalDate.of(2026, 5, 25),
        1L,
        new BigDecimal("1000.00"), 
        "Expensive Tacos", 
        SubCategory.RESTAURANT, 
        "user-1");

    private static final ExpenseEntity MAPPEED_ENTITY = new ExpenseEntity(
        null, 
        LocalDate.of(2026, 5, 25),
        1L,
        new BigDecimal("1000.00"), 
        "Expensive Tacos", 
        CategoryMapper.getCategory(SubCategory.RESTAURANT).name(),
        SubCategory.RESTAURANT.name(),
        "user-1");

    private static final ExpenseEntity CREATED_ENTITY = new ExpenseEntity(
        1L, 
        LocalDate.of(2026, 5, 25),
        1L,
        new BigDecimal("1000.00"), 
        "Expensive Tacos", 
        CategoryMapper.getCategory(SubCategory.RESTAURANT).name(),
        SubCategory.RESTAURANT.name(),
        "user-1");

    private static final Expense CREATED_DTO = new Expense(
        1L, 
        LocalDate.of(2026, 5, 25),
        1L,
        new BigDecimal("1000.00"), 
        "Expensive Tacos", 
        SubCategory.RESTAURANT,
        "user-1");

    private static final Account VALID_ACCOUNT = new Account(
        1L,
        "Debit account",
        AccountType.DEBIT,
        "USD",
        new BigDecimal("1000.00"),
        new BigDecimal("900.00"),
        "user-1");

    @Mock
    ExpenseRepository repo;

    @Mock
    AccountService accountService;

    @InjectMocks
    ExpenseService service;

    @Test
    void create_testHappyPath() {
        CreateExpenseRequest request = new CreateExpenseRequest(VALID_NEW_EXPENSE);
        when(accountService.getById(VALID_NEW_EXPENSE.accountId())).thenReturn(VALID_ACCOUNT);
        when(repo.create(MAPPEED_ENTITY)).thenReturn(CREATED_ENTITY);
        CreateExpenseResponse actualReponse = service.create(request);
        assertEquals(actualReponse.value(), CREATED_DTO);
    }

    @Test
    void create_invalidAccount() {
        CreateExpenseRequest request = new CreateExpenseRequest(VALID_NEW_EXPENSE);
        when(accountService.getById(VALID_NEW_EXPENSE.accountId())).thenReturn(new Account(
            1L,
            "Debit account",
            AccountType.DEBIT,
            "USD",
            new BigDecimal("1000.00"),
            new BigDecimal("900.00"),
            "user-2"));
        
        assertThrows(UnauthorizedAccountException.class, () -> service.create(request));
    }



    @ParameterizedTest(name = "create_testInvalidInputs-{0}")
    @MethodSource("invalidExpenses")
    void create_testInvalidInputs(String testName, Expense expense) {
        CreateExpenseRequest request = new CreateExpenseRequest(expense);
        assertThrows(ExpenseValidationException.class, () -> service.create(request));
    }

    static Stream<Arguments> invalidExpenses() {
        return Stream.of(
            Arguments.of(
                "invalid_expense_date", 
                new Expense(
                    null, 
                    null,
                    1L,
                    new BigDecimal("1000.00"), 
                    "Expensive Tacos", 
                    SubCategory.RESTAURANT, 
                    "user-1")),
            Arguments.of(
                "invalid_account_id",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    null,
                    new BigDecimal("1000.00"), 
                    "Expensive Tacos", 
                    SubCategory.RESTAURANT, 
                    "user-1")),
            Arguments.of(
                "invalid_account_amount",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    1L,
                    null, 
                    "Expensive Tacos", 
                    SubCategory.RESTAURANT, 
                    "user-1")),
            Arguments.of(
                "null_description",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    1L,
                    new BigDecimal("1000.00"), 
                    null, 
                    SubCategory.RESTAURANT, 
                    "user-1")),
            Arguments.of(
                "empty_description",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    1L,
                    new BigDecimal("1000.00"), 
                    "", 
                    SubCategory.RESTAURANT, 
                    "user-1")),
            Arguments.of(
                "invalid_subcategory",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    1L,
                    new BigDecimal("1000.00"), 
                    "Expensive Tacos", 
                    null, 
                    "user-1")),
            Arguments.of(
                "null_created_by",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    1L,
                    new BigDecimal("1000.00"), 
                    "Expensive Tacos", 
                    SubCategory.RESTAURANT, 
                    null)),
            Arguments.of(
                "empty_creted_by",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    1L,
                    new BigDecimal("1000.00"), 
                    "Expensive Tacos", 
                    SubCategory.RESTAURANT, 
                    "")));
    }
}
