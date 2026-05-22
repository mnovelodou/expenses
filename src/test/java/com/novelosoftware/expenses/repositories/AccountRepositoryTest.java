package com.novelosoftware.expenses.repositories;

import com.novelosoftware.expenses.dto.AccountType;
import com.novelosoftware.expenses.entities.AccountEntity;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final AccountRepository repo = new AccountRepository(jdbc);

    @Test
    void findByUser_returnsPaginatedEntities() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("user-1"), eq(20), eq(0)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByUser("user-1", 20, 0);

        assertEquals(1, result.size());
        assertEquals("user-1", result.get(0).createdBy());
    }

    @Test
    void countByUser_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq("user-1"))).thenReturn(5L);

        assertEquals(5L, repo.countByUser("user-1"));
    }

    @Test
    void findById_returnsEntity() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(1L))).thenReturn(List.of(anEntity(1L)));

        Optional<AccountEntity> result = repo.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().accountId());
    }

    @Test
    void findById_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq(99L))).thenReturn(List.of());

        Optional<AccountEntity> result = repo.findById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void create_insertsAndReturnsEntity() {
        var entity = anEntity(null);
        when(jdbc.queryForObject(anyString(), any(RowMapper.class), any(), any(), any(), any(), any(), any()))
            .thenReturn(anEntity(1L));

        var result = repo.create(entity);

        assertNotNull(result);
        assertEquals(1L, result.accountId());
    }

    @Test
    void update_returnsUpdatedEntity() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any(), any(), eq(1L)))
            .thenReturn(List.of(anEntity(1L)));

        Optional<AccountEntity> result = repo.update(1L, anEntity(1L));

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().accountId());
    }

    @Test
    void update_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any(), any(), eq(99L)))
            .thenReturn(List.of());

        Optional<AccountEntity> result = repo.update(99L, anEntity(null));

        assertTrue(result.isEmpty());
    }

    @Test
    void delete_returnsTrueWhenDeleted() {
        when(jdbc.update(anyString(), eq(1L))).thenReturn(1);

        assertTrue(repo.delete(1L));
    }

    @Test
    void delete_returnsFalseWhenNotFound() {
        when(jdbc.update(anyString(), eq(99L))).thenReturn(0);

        assertFalse(repo.delete(99L));
    }

    private AccountEntity anEntity(Long id) {
        return new AccountEntity(id, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1000.00"), new BigDecimal("1000.00"), null, null, "user-1");
    }
}
