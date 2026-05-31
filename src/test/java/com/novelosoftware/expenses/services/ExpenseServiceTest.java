package com.novelosoftware.expenses.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
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
import com.novelosoftware.expenses.dto.UpdateExpenseRequest;
import com.novelosoftware.expenses.dto.UpdateExpenseResponse;
import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.ExpenseValidationException;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.UnauthorizedExpenseException;
import com.novelosoftware.expenses.mappers.CategoryMapper;
import com.novelosoftware.expenses.repositories.ExpenseRepository;

/**
 * Test class for {@link ExpenseService.class}
 */
@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    private static final Long EXPENSE_ID = 31416L;

    private static final Long ACCOUNT_ID = 543412L;

    private static final Expense VALID_NEW_EXPENSE = new Expense(
        null, 
        LocalDate.of(2026, 5, 25),
        ACCOUNT_ID,
        new BigDecimal("1000.00"), 
        "Expensive Tacos", 
        SubCategory.RESTAURANT, 
        "user-1");

    private static final ExpenseEntity MAPPEED_ENTITY = new ExpenseEntity(
        null, 
        LocalDate.of(2026, 5, 25),
        ACCOUNT_ID,
        new BigDecimal("1000.00"), 
        "Expensive Tacos", 
        CategoryMapper.getCategory(SubCategory.RESTAURANT).name(),
        SubCategory.RESTAURANT.name(),
        "user-1");

    private static final ExpenseEntity CREATED_ENTITY = new ExpenseEntity(
        EXPENSE_ID, 
        LocalDate.of(2026, 5, 25),
        ACCOUNT_ID,
        new BigDecimal("1000.00"), 
        "Expensive Tacos", 
        CategoryMapper.getCategory(SubCategory.RESTAURANT).name(),
        SubCategory.RESTAURANT.name(),
        "user-1");

    private static final Expense CREATED_DTO = new Expense(
        EXPENSE_ID, 
        LocalDate.of(2026, 5, 25),
        ACCOUNT_ID,
        new BigDecimal("1000.00"), 
        "Expensive Tacos", 
        SubCategory.RESTAURANT,
        "user-1");

    private static final Account VALID_ACCOUNT = new Account(
        ACCOUNT_ID,
        "Debit account",
        AccountType.DEBIT,
        "USD",
        new BigDecimal("1000.00"),
        new BigDecimal("900.00"),
        "user-1");

    private static final Expense UPDATED_EXPENSE = VALID_NEW_EXPENSE.toBuilder()
        .expenseId(EXPENSE_ID)
        .amount(new BigDecimal("100.00"))
        .build();

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
        when(accountService.getById(VALID_NEW_EXPENSE.accountId())).thenReturn(VALID_ACCOUNT.toBuilder()
            .createdBy("user-2")
            .build());
        
        assertThrows(UnauthorizedExpenseException.class, () -> service.create(request));
    }

    @Test 
    void update_happyPath() {
        ExpenseEntity existingExpense = MAPPEED_ENTITY.toBuilder()
            .expenseId(EXPENSE_ID)
            .build();

        ExpenseEntity updatedExpenseEntity = existingExpense.toBuilder()
            .amount(new BigDecimal("100.00"))
            .build();
        
        when(accountService.getById(anyLong())).thenReturn(VALID_ACCOUNT);
        when(repo.get(anyLong())).thenReturn(Optional.of(existingExpense));
        when(repo.update(anyLong(), any(ExpenseEntity.class))).thenReturn(Optional.of(updatedExpenseEntity));

        UpdateExpenseResponse actual = service.update(EXPENSE_ID, new UpdateExpenseRequest(UPDATED_EXPENSE));

        assertEquals(UPDATED_EXPENSE, actual.value());
        verify(accountService).getById(ACCOUNT_ID);
        verify(repo).get(EXPENSE_ID);
        verify(repo).update(EXPENSE_ID, updatedExpenseEntity);
    }

    @Test
    void update_testInvalidAccount() {
        when(accountService.getById(VALID_NEW_EXPENSE.accountId())).thenReturn(VALID_ACCOUNT.toBuilder()
            .createdBy("user-2")
            .build());

        assertThrows(UnauthorizedExpenseException.class, () -> service.update(
            EXPENSE_ID, new UpdateExpenseRequest(UPDATED_EXPENSE)));
    }

    @Test 
    void update_unauthorizedExpense() {
        ExpenseEntity existingExpense = MAPPEED_ENTITY.toBuilder()
            .expenseId(EXPENSE_ID)
            .createdBy("user-2")
            .build();
        
        when(accountService.getById(anyLong())).thenReturn(VALID_ACCOUNT);
        when(repo.get(anyLong())).thenReturn(Optional.of(existingExpense));

       assertThrows(UnauthorizedExpenseException.class, () -> service.update(EXPENSE_ID, new UpdateExpenseRequest(UPDATED_EXPENSE)));

        verify(accountService).getById(ACCOUNT_ID);
        verify(repo).get(EXPENSE_ID);
    }

    @ParameterizedTest(name = "create_testInvalidInputs-{0}")
    @MethodSource("invalidExpenses")
    void create_testInvalidInputs(String testName, Expense expense) {
        CreateExpenseRequest request = new CreateExpenseRequest(expense);
        assertThrows(ExpenseValidationException.class, () -> service.create(request));
    }

    @ParameterizedTest(name = "update_testInvalidInputs-{0}")
    @MethodSource("invalidExpenses")
    void update_testInvalidInputs(String testName, Expense expense) {
        UpdateExpenseRequest updateExpenseRequest = new UpdateExpenseRequest(expense);
        assertThrows(ExpenseValidationException.class, () -> service.update(EXPENSE_ID, updateExpenseRequest));
    }

    static Stream<Arguments> invalidExpenses() {
        return Stream.of(
            Arguments.of(
                "invalid_expense_date", 
                new Expense(
                    null, 
                    null,
                    ACCOUNT_ID,
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
                    ACCOUNT_ID,
                    null, 
                    "Expensive Tacos", 
                    SubCategory.RESTAURANT, 
                    "user-1")),
            Arguments.of(
                "null_description",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    ACCOUNT_ID,
                    new BigDecimal("1000.00"), 
                    null, 
                    SubCategory.RESTAURANT, 
                    "user-1")),
            Arguments.of(
                "empty_description",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    ACCOUNT_ID,
                    new BigDecimal("1000.00"), 
                    "", 
                    SubCategory.RESTAURANT, 
                    "user-1")),
            Arguments.of(
                "invalid_subcategory",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    ACCOUNT_ID,
                    new BigDecimal("1000.00"), 
                    "Expensive Tacos", 
                    null, 
                    "user-1")),
            Arguments.of(
                "null_created_by",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    ACCOUNT_ID,
                    new BigDecimal("1000.00"), 
                    "Expensive Tacos", 
                    SubCategory.RESTAURANT, 
                    null)),
            Arguments.of(
                "empty_creted_by",
                new Expense(
                    null, 
                    LocalDate.of(2026, 5, 25),
                    ACCOUNT_ID,
                    new BigDecimal("1000.00"), 
                    "Expensive Tacos", 
                    SubCategory.RESTAURANT, 
                    "")));
    }
}
