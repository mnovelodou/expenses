package com.novelosoftware.expenses.services;

import com.novelosoftware.expenses.dto.Account;
import com.novelosoftware.expenses.dto.AccountType;
import com.novelosoftware.expenses.dto.CreateAccountRequest;
import com.novelosoftware.expenses.dto.UpdateAccountRequest;
import com.novelosoftware.expenses.entities.AccountEntity;
import com.novelosoftware.expenses.exceptions.AccountServiceExceptions.AccountNotFoundException;
import com.novelosoftware.expenses.exceptions.AccountServiceExceptions.AccountValidationException;
import com.novelosoftware.expenses.repositories.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for {@link AccountService.class}
 */
class AccountServiceTest {

    private static final Account VALID_ACCOUNT = new Account(
                null,
                "Checking", 
                AccountType.DEBIT, 
                "USD", 
                new BigDecimal("1000.00"),
                null,
                "user-1");

    private static final Account INVALID_ACCOUNT_BAD_NAME = new Account(
                null,
                "", 
                AccountType.DEBIT, 
                "USD", 
                new BigDecimal("1000.00"),
                null,
                "user-1");

    private static final Account INVALID_ACCOUNT_BAD_ACCOUNT_TYPE = new Account(
                null,
                "Checking", 
                null, 
                "USD", 
                new BigDecimal("1000.00"),
                null,
                "user-1");

    private final AccountRepository repo = mock(AccountRepository.class);
    private final AccountService service = new AccountService(repo);

    @Test
    void getByUser_returnsPaginatedAccounts() {
        when(repo.findByUser("user-1", 20, 0)).thenReturn(List.of(anEntity(1L)));
        when(repo.countByUser("user-1")).thenReturn(1L);

        var result = service.getByUser("user-1", 0, 20);

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
    void getById_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    void create_persistsAndReturnsResponse() {
        var createAccountRequest = new CreateAccountRequest(VALID_ACCOUNT);
        when(repo.create(any())).thenReturn(anEntity(1L));

        var result = service.create(createAccountRequest);

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
        assertThrows(AccountValidationException.class, () -> service.create(createAccountRequest));
    }

    @ParameterizedTest(name = "update_testInvalidInputs-{0}")
    @MethodSource("invalidAccounts")
    void update_testInvalidInputs(String testName, Account invalidAccount) {
        var createAccountRequest = new UpdateAccountRequest(invalidAccount);
        assertThrows(AccountValidationException.class, () -> service.update(99L, createAccountRequest));
    }

    @Test
    void update_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        var request = new com.novelosoftware.expenses.dto.UpdateAccountRequest(VALID_ACCOUNT);

        assertThrows(AccountNotFoundException.class, () -> service.update(99L, request));
    }

    @Test
    void delete_throwsWhenNotFound() {
        when(repo.delete(99L)).thenReturn(false);

        assertThrows(AccountNotFoundException.class, () -> service.delete(99L));
    }

    private AccountEntity anEntity(Long id) {
        return new AccountEntity(id, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1000.00"), new BigDecimal("1000.00"), null, null, "user-1");
    }
}
