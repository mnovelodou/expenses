package com.novelosoftware.expenses.services;

import com.novelosoftware.expenses.dto.Account;
import com.novelosoftware.expenses.dto.AccountType;
import com.novelosoftware.expenses.dto.CreateAccountRequest;
import com.novelosoftware.expenses.dto.UpdateAccountRequest;
import com.novelosoftware.expenses.entities.AccountEntity;
import com.novelosoftware.expenses.exceptions.AccountServiceExceptions.AccountNotFoundException;
import com.novelosoftware.expenses.exceptions.AccountServiceExceptions.AccountValidationException;
import com.novelosoftware.expenses.repositories.AccountRepository;
import com.novelosoftware.expenses.repositories.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link AccountService.class}
 */
class AccountServiceTest {

    private static final String CALLER = "user-1";

    private static final Account VALID_ACCOUNT = new Account(
                null,
                "Checking",
                AccountType.DEBIT,
                "USD",
                new BigDecimal("1000.00"),
                null,
                "user-1",
                LocalDate.of(2026, 6, 1),
                null);

    private static final Account INVALID_ACCOUNT_BAD_NAME = new Account(
                null,
                "",
                AccountType.DEBIT,
                "USD",
                new BigDecimal("1000.00"),
                null,
                "user-1",
                LocalDate.of(2026, 6, 1),
                null);

    private static final Account INVALID_ACCOUNT_BAD_ACCOUNT_TYPE = new Account(
                null,
                "Checking",
                null,
                "USD",
                new BigDecimal("1000.00"),
                null,
                "user-1",
                LocalDate.of(2026, 6, 1),
                null);

    private final AccountRepository repo = mock(AccountRepository.class);
    private final ExpenseRepository expenseRepo = mock(ExpenseRepository.class);
    private final AccountService service = new AccountService(repo, expenseRepo);

    @Test
    void getByUser_returnsPaginatedAccounts() {
        when(repo.findByUser("user-1", 20, 0)).thenReturn(List.of(anEntity(1L)));
        when(repo.countByUser("user-1")).thenReturn(1L);

        var result = service.findByUser(CALLER, "user-1", 0, 20);

        assertEquals(1, result.content().size());
        assertEquals(1L, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals(1L, result.content().get(0).accountId());
    }

    @Test
    void getById_returnsAccount() {
        when(repo.findById(1L)).thenReturn(Optional.of(anEntity(1L)));

        var result = service.getById(1L);

        assertEquals(1L, result.accountId());
    }

    @Test
    void getById_withoutGap_doesNotComputeGap() {
        when(repo.findById(1L)).thenReturn(Optional.of(anEntityWithPeriodStart(1L)));

        var result = service.getById(1L, false);

        assertNull(result.gap());
        verify(expenseRepo, never()).sumByAccountSince(any(), any());
    }

    @Test
    void getById_withGap_computesGapFromExpensesSincePeriodStart() {
        // initial 1000, current 1500, expenses since period_start = 200 -> gap = 1500 - 1000 - 200 = 300
        when(repo.findById(1L)).thenReturn(Optional.of(anEntityWithPeriodStart(1L)));
        when(expenseRepo.sumByAccountSince(1L, LocalDate.of(2026, 6, 1)))
            .thenReturn(new BigDecimal("200.00"));

        var result = service.getById(1L, true);

        assertEquals(new BigDecimal("300.00"), result.gap());
    }

    @Test
    void getById_withGap_nullPeriodStart_returnsNullGapWithoutQuerying() {
        when(repo.findById(1L)).thenReturn(Optional.of(anEntity(1L))); // period_start is null

        var result = service.getById(1L, true);

        assertNull(result.gap());
        verify(expenseRepo, never()).sumByAccountSince(any(), any());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    void create_persistsAndReturnsResponse() {
        var createAccountRequest = new CreateAccountRequest(VALID_ACCOUNT);
        when(repo.create(any())).thenReturn(anEntity(1L));

        var result = service.create(createAccountRequest, CALLER);

        assertEquals(1L, result.value().accountId());
        verify(repo).create(any());
    }

    static Stream<Arguments> invalidAccounts() {
        return Stream.of(
            Arguments.of("account_with_empty_name", INVALID_ACCOUNT_BAD_NAME), 
            Arguments.of("account_with_empty_account_type", INVALID_ACCOUNT_BAD_ACCOUNT_TYPE));
    }

    @ParameterizedTest(name = "create_testInvalidInputs-{0}")
    @MethodSource("invalidAccounts")
    void create_testInvalidInputs(String testName, Account invalidAccount) {
        var createAccountRequest = new CreateAccountRequest(invalidAccount);
        assertThrows(AccountValidationException.class, () -> service.create(createAccountRequest, CALLER));
    }

    @ParameterizedTest(name = "update_testInvalidInputs-{0}")
    @MethodSource("invalidAccounts")
    void update_testInvalidInputs(String testName, Account invalidAccount) {
        var createAccountRequest = new UpdateAccountRequest(invalidAccount);
        assertThrows(AccountValidationException.class, () -> service.update(99L, createAccountRequest, CALLER));
    }

    @Test
    void update_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        var request = new com.novelosoftware.expenses.dto.UpdateAccountRequest(VALID_ACCOUNT);

        assertThrows(AccountNotFoundException.class, () -> service.update(99L, request, CALLER));
    }

    @Test
    void update_newInitialAmount_persistsItWithoutAffectingCurrentAmount() {
        var existing = new AccountEntity(1L, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1000.00"), new BigDecimal("1200.00"), null, null, "user-1", null);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));

        var requestAccount = new Account(1L, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1500.00"), new BigDecimal("800.00"), "user-1", null, null);
        when(repo.update(eq(1L), any())).thenAnswer(inv -> Optional.of((AccountEntity) inv.getArgument(1)));

        var result = service.update(1L, new UpdateAccountRequest(requestAccount), CALLER);

        assertEquals(new BigDecimal("1500.00"), result.value().initialAmount());
        assertEquals(new BigDecimal("800.00"), result.value().currentAmount());
    }

    @Test
    void update_newInitialAmountWithoutCurrentAmount_preservesStoredCurrentAmount() {
        var existing = new AccountEntity(1L, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1000.00"), new BigDecimal("1200.00"), null, null, "user-1", null);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));

        var requestAccount = new Account(1L, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1500.00"), null, "user-1", null, null);
        when(repo.update(eq(1L), any())).thenAnswer(inv -> Optional.of((AccountEntity) inv.getArgument(1)));

        var result = service.update(1L, new UpdateAccountRequest(requestAccount), CALLER);

        assertEquals(new BigDecimal("1500.00"), result.value().initialAmount());
        assertEquals(new BigDecimal("1200.00"), result.value().currentAmount());
    }

    @Test
    void update_nullInitialAmount_preservesStoredInitialAmount() {
        var existing = new AccountEntity(1L, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1000.00"), new BigDecimal("1200.00"), null, null, "user-1", null);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));

        var requestAccount = new Account(1L, "Checking", AccountType.DEBIT, "USD",
            null, new BigDecimal("900.00"), "user-1", null, null);
        when(repo.update(eq(1L), any())).thenAnswer(inv -> Optional.of((AccountEntity) inv.getArgument(1)));

        var result = service.update(1L, new UpdateAccountRequest(requestAccount), CALLER);

        assertEquals(new BigDecimal("1000.00"), result.value().initialAmount());
        assertEquals(new BigDecimal("900.00"), result.value().currentAmount());
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.delete(99L, CALLER));
    }

    @Test
    void delete_notOwned_throwsAccountNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.of(anEntity(1L))); // owned by user-1
        assertThrows(AccountNotFoundException.class, () -> service.delete(1L, "intruder"));
    }

    @Test
    void getById_notOwned_throwsAccountNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.of(anEntity(1L))); // owned by user-1
        assertThrows(AccountNotFoundException.class, () -> service.getById(1L, false, "intruder"));
    }

    @Test
    void update_notOwned_throwsAccountNotFound() {
        when(repo.findById(1L)).thenReturn(Optional.of(anEntity(1L))); // stored row owned by user-1
        // Body owner matches the caller, so impersonation passes; the stored-row check denies.
        var request = new UpdateAccountRequest(VALID_ACCOUNT.toBuilder().createdBy("intruder").build());
        assertThrows(AccountNotFoundException.class, () -> service.update(1L, request, "intruder"));
    }

    @Test
    void create_impersonatingAnotherUser_throwsUnauthorized() {
        var request = new CreateAccountRequest(VALID_ACCOUNT); // createdBy user-1
        assertThrows(com.novelosoftware.expenses.exceptions.AccountServiceExceptions.UnauthorizedAccountException.class,
            () -> service.create(request, "intruder"));
    }

    @Test
    void findByUser_requestedUserNotCaller_throwsAccountNotFound() {
        assertThrows(AccountNotFoundException.class,
            () -> service.findByUser(CALLER, "someone-else", 0, 20));
    }

    private AccountEntity anEntity(Long id) {
        return new AccountEntity(id, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1000.00"), new BigDecimal("1000.00"), null, null, "user-1", null);
    }

    private AccountEntity anEntityWithPeriodStart(Long id) {
        return new AccountEntity(id, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1000.00"), new BigDecimal("1500.00"), null, null, "user-1",
            LocalDate.of(2026, 6, 1));
    }
}
