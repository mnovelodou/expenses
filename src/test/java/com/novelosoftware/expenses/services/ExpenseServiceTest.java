package com.novelosoftware.expenses.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.novelosoftware.expenses.dto.Account;
import com.novelosoftware.expenses.dto.AccountType;
import com.novelosoftware.expenses.dto.BulkCreateExpensesRequest;
import com.novelosoftware.expenses.dto.CreateExpenseRequest;
import com.novelosoftware.expenses.dto.CreateExpenseResponse;
import com.novelosoftware.expenses.dto.CursorPageResponse;
import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.SubCategory;
import com.novelosoftware.expenses.dto.UpdateExpenseRequest;
import com.novelosoftware.expenses.dto.UpdateExpenseResponse;
import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.exceptions.AccountServiceExceptions;
import com.novelosoftware.expenses.exceptions.AccountServiceExceptions.AccountNotFoundException;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.ExpenseNotFoundException;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.ExpenseValidationException;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.InvalidCursorException;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.UnauthorizedExpenseException;
import com.novelosoftware.expenses.mappers.CategoryMapper;
import com.novelosoftware.expenses.repositories.ExpenseRepository;
import com.novelosoftware.expenses.util.ExpenseCursor;

/**
 * Test class for {@link ExpenseService.class}
 */
@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    private static final Long EXPENSE_ID = 31416L;

    /** Owner of the test fixtures; also the authenticated caller in most cases. */
    private static final String CALLER = "user-1";

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
        "user-1",
        null,
        null);

    private static final Expense UPDATED_EXPENSE = VALID_NEW_EXPENSE.toBuilder()
        .expenseId(EXPENSE_ID)
        .amount(new BigDecimal("100.00"))
        .build();

    // Fixed clock: "now" = 2026-06-02, so last month = May 2026
    private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-06-02T00:00:00Z"), ZoneOffset.UTC);

    private static final LocalDate LAST_MONTH_START =
        YearMonth.now(FIXED_CLOCK).minusMonths(1).atDay(1);           // 2026-05-01
    private static final LocalDate LAST_MONTH_END =
        YearMonth.now(FIXED_CLOCK).minusMonths(1).atEndOfMonth();     // 2026-05-31

    @Mock
    ExpenseRepository repo;

    @Mock
    AccountService accountService;

    ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(repo, accountService, FIXED_CLOCK);
    }

    // -------------------------------------------------------------------------
    // getById
    // -------------------------------------------------------------------------

    @Test
    void getById_found_returnsMappedDto() {
        when(repo.get(EXPENSE_ID)).thenReturn(Optional.of(CREATED_ENTITY));
        Expense result = service.getById(EXPENSE_ID, CALLER);
        assertEquals(CREATED_DTO, result);
        verify(repo).get(EXPENSE_ID);
    }

    @Test
    void getById_notFound_throwsExpenseNotFoundException() {
        when(repo.get(EXPENSE_ID)).thenReturn(Optional.empty());
        assertThrows(ExpenseNotFoundException.class, () -> service.getById(EXPENSE_ID, CALLER));
    }

    // -------------------------------------------------------------------------
    // delete
    // -------------------------------------------------------------------------

    @Test
    void delete_success_doesNotThrow() {
        when(repo.get(EXPENSE_ID)).thenReturn(Optional.of(CREATED_ENTITY));
        when(repo.delete(EXPENSE_ID)).thenReturn(true);
        service.delete(EXPENSE_ID, CALLER);
        verify(repo).delete(EXPENSE_ID);
    }

    @Test
    void delete_notFound_throwsExpenseNotFoundException() {
        when(repo.get(EXPENSE_ID)).thenReturn(Optional.empty());
        assertThrows(ExpenseNotFoundException.class, () -> service.delete(EXPENSE_ID, CALLER));
    }

    @Test
    void delete_notOwned_throwsExpenseNotFoundException() {
        ExpenseEntity otherUsers = CREATED_ENTITY.toBuilder().createdBy("user-2").build();
        when(repo.get(EXPENSE_ID)).thenReturn(Optional.of(otherUsers));
        assertThrows(ExpenseNotFoundException.class, () -> service.delete(EXPENSE_ID, CALLER));
    }

    @Test
    void getById_notOwned_throwsExpenseNotFoundException() {
        ExpenseEntity otherUsers = CREATED_ENTITY.toBuilder().createdBy("user-2").build();
        when(repo.get(EXPENSE_ID)).thenReturn(Optional.of(otherUsers));
        assertThrows(ExpenseNotFoundException.class, () -> service.getById(EXPENSE_ID, CALLER));
    }

    @Test
    void create_impersonatingAnotherUser_throwsUnauthorized() {
        CreateExpenseRequest request = new CreateExpenseRequest(VALID_NEW_EXPENSE);
        // VALID_NEW_EXPENSE.createdBy is "user-1"; caller is someone else.
        assertThrows(UnauthorizedExpenseException.class, () -> service.create(request, "intruder"));
    }

    @Test
    void listByUser_requestedUserNotCaller_throwsExpenseNotFoundException() {
        assertThrows(ExpenseNotFoundException.class,
            () -> service.listByUser(CALLER, "someone-else", null, null, null, null, null, null, null));
    }

    @Test
    void create_testHappyPath() {
        CreateExpenseRequest request = new CreateExpenseRequest(VALID_NEW_EXPENSE);
        when(accountService.getById(eq(VALID_NEW_EXPENSE.accountId()), eq(false), eq(CALLER))).thenReturn(VALID_ACCOUNT);
        when(repo.create(MAPPEED_ENTITY)).thenReturn(CREATED_ENTITY);
        CreateExpenseResponse actualReponse = service.create(request, CALLER);
        assertEquals(actualReponse.value(), CREATED_DTO);
    }

    @Test
    void create_accountNotOwned_throwsAccountNotFound() {
        // The ownership-checked account accessor hides a non-owned (or missing) account as 404.
        CreateExpenseRequest request = new CreateExpenseRequest(VALID_NEW_EXPENSE);
        when(accountService.getById(eq(VALID_NEW_EXPENSE.accountId()), eq(false), eq(CALLER)))
            .thenThrow(AccountServiceExceptions.createAccountNotFoundException(VALID_NEW_EXPENSE.accountId()));

        assertThrows(AccountNotFoundException.class, () -> service.create(request, CALLER));
    }

    @Test
    void update_happyPath() {
        ExpenseEntity existingExpense = MAPPEED_ENTITY.toBuilder()
            .expenseId(EXPENSE_ID)
            .build();

        ExpenseEntity updatedExpenseEntity = existingExpense.toBuilder()
            .amount(new BigDecimal("100.00"))
            .build();

        when(accountService.getById(anyLong(), anyBoolean(), anyString())).thenReturn(VALID_ACCOUNT);
        when(repo.get(anyLong())).thenReturn(Optional.of(existingExpense));
        when(repo.update(anyLong(), any(ExpenseEntity.class))).thenReturn(Optional.of(updatedExpenseEntity));

        UpdateExpenseResponse actual = service.update(EXPENSE_ID, new UpdateExpenseRequest(UPDATED_EXPENSE), CALLER);

        assertEquals(UPDATED_EXPENSE, actual.value());
        verify(accountService).getById(ACCOUNT_ID, false, CALLER);
        verify(repo).get(EXPENSE_ID);
        verify(repo).update(EXPENSE_ID, updatedExpenseEntity);
    }

    @Test
    void update_accountNotOwned_throwsAccountNotFound() {
        when(accountService.getById(eq(VALID_NEW_EXPENSE.accountId()), eq(false), eq(CALLER)))
            .thenThrow(AccountServiceExceptions.createAccountNotFoundException(VALID_NEW_EXPENSE.accountId()));

        assertThrows(AccountNotFoundException.class, () -> service.update(EXPENSE_ID, new UpdateExpenseRequest(UPDATED_EXPENSE), CALLER));
    }

    @Test
    void update_notOwned_throwsExpenseNotFoundException() {
        ExpenseEntity existingExpense = MAPPEED_ENTITY.toBuilder()
            .expenseId(EXPENSE_ID)
            .createdBy("user-2")
            .build();

        when(accountService.getById(anyLong(), anyBoolean(), anyString())).thenReturn(VALID_ACCOUNT);
        when(repo.get(anyLong())).thenReturn(Optional.of(existingExpense));

       assertThrows(ExpenseNotFoundException.class, () -> service.update(EXPENSE_ID, new UpdateExpenseRequest(UPDATED_EXPENSE), CALLER));

        verify(accountService).getById(ACCOUNT_ID, false, CALLER);
        verify(repo).get(EXPENSE_ID);
    }

    @Test
    void update_impersonatingAnotherUser_throwsUnauthorized() {
        // body.createdBy is "user-1"; caller is someone else.
        assertThrows(UnauthorizedExpenseException.class,
            () -> service.update(EXPENSE_ID, new UpdateExpenseRequest(UPDATED_EXPENSE), "intruder"));
    }

    @ParameterizedTest(name = "create_testInvalidInputs-{0}")
    @MethodSource("invalidExpenses")
    void create_testInvalidInputs(String testName, Expense expense) {
        CreateExpenseRequest request = new CreateExpenseRequest(expense);
        assertThrows(ExpenseValidationException.class, () -> service.create(request, CALLER));
    }

    @ParameterizedTest(name = "update_testInvalidInputs-{0}")
    @MethodSource("invalidExpenses")
    void update_testInvalidInputs(String testName, Expense expense) {
        UpdateExpenseRequest updateExpenseRequest = new UpdateExpenseRequest(expense);
        assertThrows(ExpenseValidationException.class, () -> service.update(EXPENSE_ID, updateExpenseRequest, CALLER));
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

    // -------------------------------------------------------------------------
    // listByUser — date defaults (DateWindowResolver is tested separately;
    // these verify the service wires dates through correctly)
    // -------------------------------------------------------------------------

    @Test
    void listByUser_bothDatesAbsent_defaultsToLastCalendarMonth() {
        when(repo.findByFiltersCursor(eq("user-1"), eq(LAST_MONTH_START), eq(LAST_MONTH_END),
            isNull(), isNull(), isNull(), eq(20), isNull()))
            .thenReturn(List.of());

        CursorPageResponse<Expense> result = service.listByUser(CALLER, "user-1", null, null, null, null, null, null, null);

        assertNotNull(result);
        assertNull(result.nextCursor());
    }

    @Test
    void listByUser_onlyStartDateProvided_endDateDerived() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate expectedEnd = start.plusMonths(1);

        when(repo.findByFiltersCursor(eq("user-1"), eq(start), eq(expectedEnd),
            isNull(), isNull(), isNull(), anyInt(), isNull()))
            .thenReturn(List.of());

        service.listByUser(CALLER, "user-1", start, null, null, null, null, null, null);
    }

    @Test
    void listByUser_onlyEndDateProvided_startDateDerived() {
        LocalDate end = LocalDate.of(2026, 5, 1);
        LocalDate expectedStart = end.minusMonths(1);

        when(repo.findByFiltersCursor(eq("user-1"), eq(expectedStart), eq(end),
            isNull(), isNull(), isNull(), anyInt(), isNull()))
            .thenReturn(List.of());

        service.listByUser(CALLER, "user-1", null, end, null, null, null, null, null);
    }

    // -------------------------------------------------------------------------
    // listByUser — limit validation
    // -------------------------------------------------------------------------

    @ParameterizedTest(name = "listByUser_invalidLimit_{0}_throws400")
    @ValueSource(ints = {-5, -1, 0, 101, 200})
    void listByUser_invalidLimit_throws400(int limit) {
        assertThrows(ExpenseValidationException.class,
            () -> service.listByUser(CALLER, "user-1", null, null, limit, null, null, null, null));
    }

    @Test
    void listByUser_limitAtMaximum_isAllowed() {
        when(repo.findByFiltersCursor(eq("user-1"), any(), any(),
            isNull(), isNull(), isNull(), eq(100), isNull()))
            .thenReturn(List.of());

        service.listByUser(CALLER, "user-1", null, null, 100, null, null, null, null);
    }

    @Test
    void listByUser_limitAbsent_defaultsTo20() {
        when(repo.findByFiltersCursor(eq("user-1"), any(), any(),
            isNull(), isNull(), isNull(), eq(20), isNull()))
            .thenReturn(List.of());

        service.listByUser(CALLER, "user-1", null, null, null, null, null, null, null);
    }

    // -------------------------------------------------------------------------
    // listByUser — cursor validation
    // -------------------------------------------------------------------------

    @Test
    void listByUser_cursorDateOutsideRange_throwsInvalidCursorException() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 5, 1);
        String cursor = ExpenseCursor.encode(LocalDate.of(2026, 3, 1), 10L);

        assertThrows(InvalidCursorException.class,
            () -> service.listByUser(CALLER, "user-1", start, end, null, cursor, null, null, null));
    }

    // -------------------------------------------------------------------------
    // listByUser — filter mutual exclusivity
    // -------------------------------------------------------------------------

    @Test
    void listByUser_categoryAndSubcategoryBothProvided_throws400() {
        assertThrows(ExpenseValidationException.class,
            () -> service.listByUser(CALLER, "user-1", null, null, null, null, "Food", "Groceries", null));
    }

    // -------------------------------------------------------------------------
    // listByUser — filters passed through to repo
    // -------------------------------------------------------------------------

    @Test
    void listByUser_categoryFilter_passedToRepo() {
        when(repo.findByFiltersCursor(eq("user-1"), any(), any(),
            eq("Food"), isNull(), isNull(), anyInt(), isNull()))
            .thenReturn(List.of());

        service.listByUser(CALLER, "user-1", null, null, null, null, "Food", null, null);

        verify(repo).findByFiltersCursor(eq("user-1"), any(), any(),
            eq("Food"), isNull(), isNull(), anyInt(), isNull());
    }

    @Test
    void listByUser_subcategoryFilter_passedToRepo() {
        when(repo.findByFiltersCursor(eq("user-1"), any(), any(),
            isNull(), eq("Groceries"), isNull(), anyInt(), isNull()))
            .thenReturn(List.of());

        service.listByUser(CALLER, "user-1", null, null, null, null, null, "Groceries", null);

        verify(repo).findByFiltersCursor(eq("user-1"), any(), any(),
            isNull(), eq("Groceries"), isNull(), anyInt(), isNull());
    }

    @Test
    void listByUser_accountIdFilter_passedToRepo() {
        when(repo.findByFiltersCursor(eq("user-1"), any(), any(),
            isNull(), isNull(), eq(3L), anyInt(), isNull()))
            .thenReturn(List.of());

        service.listByUser(CALLER, "user-1", null, null, null, null, null, null, 3L);

        verify(repo).findByFiltersCursor(eq("user-1"), any(), any(),
            isNull(), isNull(), eq(3L), anyInt(), isNull());
    }

    // -------------------------------------------------------------------------
    // listByUser — nextCursor
    // -------------------------------------------------------------------------

    @Test
    void listByUser_resultsEqualLimit_nextCursorPresent() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 5, 1);

        List<ExpenseEntity> entities = List.of(
            anEntity(10L, LocalDate.of(2026, 4, 20)),
            anEntity(9L,  LocalDate.of(2026, 4, 15)));

        when(repo.findByFiltersCursor(eq("user-1"), eq(start), eq(end),
            isNull(), isNull(), isNull(), eq(2), isNull()))
            .thenReturn(entities);

        CursorPageResponse<Expense> result = service.listByUser(CALLER, "user-1", start, end, 2, null, null, null, null);

        assertNotNull(result.nextCursor());
    }

    @Test
    void listByUser_resultsLessThanLimit_nextCursorNull() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 5, 1);

        when(repo.findByFiltersCursor(eq("user-1"), eq(start), eq(end),
            isNull(), isNull(), isNull(), eq(20), isNull()))
            .thenReturn(List.of(anEntity(5L, LocalDate.of(2026, 4, 10))));

        CursorPageResponse<Expense> result = service.listByUser(CALLER, "user-1", start, end, null, null, null, null, null);

        assertNull(result.nextCursor());
    }

    // -------------------------------------------------------------------------
    // bulkCreate
    // -------------------------------------------------------------------------

    @Test
    void bulkCreate_success_returnsAllCreatedExpenses() {
        CreateExpenseRequest request = new CreateExpenseRequest(VALID_NEW_EXPENSE);
        var bulkRequest = new BulkCreateExpensesRequest(List.of(request, request));
        List<ExpenseEntity> inserted = List.of(CREATED_ENTITY, CREATED_ENTITY);

        when(accountService.getById(eq(VALID_NEW_EXPENSE.accountId()), eq(false), eq(CALLER))).thenReturn(VALID_ACCOUNT);
        when(repo.bulkInsert(List.of(MAPPEED_ENTITY, MAPPEED_ENTITY))).thenReturn(inserted);

        List<CreateExpenseResponse> responses = service.bulkCreate(bulkRequest, CALLER);

        assertEquals(2, responses.size());
        assertEquals(CREATED_DTO, responses.get(0).value());
        assertEquals(CREATED_DTO, responses.get(1).value());
    }

    @Test
    void bulkCreate_oneItemAccountNotOwned_throwsAccountNotFound() {
        CreateExpenseRequest request = new CreateExpenseRequest(VALID_NEW_EXPENSE);
        when(accountService.getById(eq(VALID_NEW_EXPENSE.accountId()), eq(false), eq(CALLER)))
            .thenThrow(AccountServiceExceptions.createAccountNotFoundException(VALID_NEW_EXPENSE.accountId()));

        assertThrows(AccountNotFoundException.class,
            () -> service.bulkCreate(new BulkCreateExpensesRequest(List.of(request)), CALLER));
    }

    @Test
    void bulkCreate_nullPayload_throwsValidationException() {
        assertThrows(ExpenseValidationException.class,
            () -> service.bulkCreate(new BulkCreateExpensesRequest(List.of(new CreateExpenseRequest(null))), CALLER));
    }

    @Test
    void bulkCreate_nullRequest_throwsValidationException() {
        assertThrows(ExpenseValidationException.class, () -> service.bulkCreate(null, CALLER));
    }

    @Test
    void bulkCreate_emptyList_throwsValidationException() {
        assertThrows(ExpenseValidationException.class,
            () -> service.bulkCreate(new BulkCreateExpensesRequest(List.of()), CALLER));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ExpenseEntity anEntity(Long id, LocalDate date) {
        return new ExpenseEntity(id, date, ACCOUNT_ID, new BigDecimal("50.00"), "Lunch",
            CategoryMapper.getCategory(SubCategory.RESTAURANT).name(),
            SubCategory.RESTAURANT.name(), "user-1");
    }
}
