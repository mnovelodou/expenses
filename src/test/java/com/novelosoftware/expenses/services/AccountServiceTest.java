package com.novelosoftware.expenses.services;

import com.novelosoftware.expenses.dto.CreateAccountRequest;
import com.novelosoftware.expenses.entities.AccountEntity;
import com.novelosoftware.expenses.enums.AccountType;
import com.novelosoftware.expenses.exceptions.AccountNotFoundException;
import com.novelosoftware.expenses.mappers.AccountMapper;
import com.novelosoftware.expenses.repositories.AccountRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private final AccountRepository repo = mock(AccountRepository.class);
    private final AccountMapper mapper = new AccountMapper();
    private final AccountService service = new AccountService(repo, mapper);

    @Test
    void getAll_returnsMappedResponses() {
        when(repo.findAll()).thenReturn(List.of(anEntity(1L)));

        var result = service.getAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Checking", result.get(0).name());
    }

    @Test
    void getById_returnsAccount() {
        when(repo.findById(1L)).thenReturn(Optional.of(anEntity(1L)));

        var result = service.getById(1L);

        assertEquals(1L, result.id());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> service.getById(99L));
    }

    @Test
    void create_persistsAndReturnsResponse() {
        var request = new CreateAccountRequest("Checking", AccountType.DEBIT, "USD", new BigDecimal("1000.00"));
        when(repo.create(any())).thenReturn(anEntity(1L));

        var result = service.create(request, "user-1");

        assertEquals(1L, result.value().id());
        verify(repo).create(any());
    }

    @Test
    void update_throwsWhenNotFound() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
        var request = new com.novelosoftware.expenses.dto.UpdateAccountRequest(
            "Checking", AccountType.DEBIT, "USD", new BigDecimal("1000.00"));

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
