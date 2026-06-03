package com.novelosoftware.expenses.repositories;

import com.novelosoftware.expenses.entities.ExpenseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExpenseRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ExpenseRepository repo = new ExpenseRepository(jdbc);

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END   = LocalDate.of(2026, 1, 31);

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    @Test
    void create_insertsAndReturnsEntity() {
        var entity = anEntity(null);
        when(jdbc.queryForObject(anyString(), any(RowMapper.class),
            any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(anEntity(1L));

        var result = repo.create(entity);

        assertNotNull(result);
        assertEquals(1L, result.expenseId());
        verify(jdbc).queryForObject(anyString(), any(RowMapper.class),
            any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void update_returnsUpdatedEntity() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            any(), any(), any(), any(), any(), any(), eq(1L)))
            .thenReturn(List.of(anEntity(1L)));

        Optional<ExpenseEntity> result = repo.update(1L, anEntity(1L));

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().expenseId());
    }

    @Test
    void update_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            any(), any(), any(), any(), any(), any(), eq(99L)))
            .thenReturn(List.of());

        Optional<ExpenseEntity> result = repo.update(99L, anEntity(null));

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

    // -------------------------------------------------------------------------
    // findByUser
    // -------------------------------------------------------------------------

    @Test
    void findByUser_returnsPagedResults() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            eq("user-1"), eq(START), eq(END), eq(20), eq(0)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByUser("user-1", START, END, 20, 0);

        assertEquals(1, result.size());
        assertEquals("user-1", result.get(0).createdBy());
    }

    @Test
    void countByUser_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class),
            eq("user-1"), eq(START), eq(END)))
            .thenReturn(5L);

        assertEquals(5L, repo.countByUser("user-1", START, END));
    }

    @Test
    void countByUser_returnsZeroWhenNull() {
        when(jdbc.queryForObject(anyString(), eq(Long.class),
            eq("user-1"), eq(START), eq(END)))
            .thenReturn(null);

        assertEquals(0L, repo.countByUser("user-1", START, END));
    }

    // -------------------------------------------------------------------------
    // findByAccount
    // -------------------------------------------------------------------------

    @Test
    void findByAccount_returnsPagedResults() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            eq("user-1"), eq(1L), eq(START), eq(END), eq(20), eq(0)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByAccount("user-1", 1L, START, END, 20, 0);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).accountId());
    }

    @Test
    void countByAccount_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class),
            eq("user-1"), eq(1L), eq(START), eq(END)))
            .thenReturn(3L);

        assertEquals(3L, repo.countByAccount("user-1", 1L, START, END));
    }

    // -------------------------------------------------------------------------
    // findByCategory
    // -------------------------------------------------------------------------

    @Test
    void findByCategory_returnsPagedResults() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            eq("user-1"), eq("Food"), eq(START), eq(END), eq(20), eq(0)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByCategory("user-1", "Food", START, END, 20, 0);

        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).category());
    }

    @Test
    void countByCategory_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class),
            eq("user-1"), eq("Food"), eq(START), eq(END)))
            .thenReturn(7L);

        assertEquals(7L, repo.countByCategory("user-1", "Food", START, END));
    }

    // -------------------------------------------------------------------------
    // findBySubcategory
    // -------------------------------------------------------------------------

    @Test
    void findBySubcategory_returnsPagedResults() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            eq("user-1"), eq("Restaurants"), eq(START), eq(END), eq(20), eq(0)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findBySubcategory("user-1", "Restaurants", START, END, 20, 0);

        assertEquals(1, result.size());
        assertEquals("Restaurants", result.get(0).subcategory());
    }

    @Test
    void countBySubcategory_returnsCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class),
            eq("user-1"), eq("Restaurants"), eq(START), eq(END)))
            .thenReturn(2L);

        assertEquals(2L, repo.countBySubcategory("user-1", "Restaurants", START, END));
    }

    // -------------------------------------------------------------------------
    // Get
    // -------------------------------------------------------------------------

    @Test
    void get_happyPath() {
        var id = 1L;
        when(jdbc.query(ExpenseRepository.GET_SQL, ExpenseRepository.MAPPER, id))
            .thenReturn(List.of(anEntity(id)));
        
        assertTrue(repo.get(id).isPresent());
    }

    @Test
    void get_notFound() {
        var id = 1L;
        when(jdbc.query(ExpenseRepository.GET_SQL, ExpenseRepository.MAPPER, id))
            .thenReturn(List.of());
        
        assertTrue(repo.get(id).isEmpty());
    }

    // -------------------------------------------------------------------------
    // findByUserCursor
    // -------------------------------------------------------------------------

    @Test
    void findByUserCursor_firstPage_usesFirstPageSql() {
        when(jdbc.query(eq(ExpenseRepository.FIND_BY_USER_CURSOR_SQL), any(RowMapper.class),
            eq("user-1"), eq(START), eq(END), eq(20)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByUserCursor("user-1", START, END, 20, null);

        assertEquals(1, result.size());
    }

    @Test
    void findByUserCursor_withCursor_usesCursorSql() {
        LocalDate cursorDate = LocalDate.of(2026, 1, 15);
        long cursorId = 5L;
        var cursor = new com.novelosoftware.expenses.util.ExpenseCursor.DecodedCursor(cursorDate, cursorId);

        when(jdbc.query(eq(ExpenseRepository.FIND_BY_USER_CURSOR_WITH_CURSOR_SQL), any(RowMapper.class),
            eq("user-1"), eq(START), eq(END),
            eq(cursorDate), eq(cursorDate), eq(cursorId),
            eq(20)))
            .thenReturn(List.of(anEntity(3L)));

        var result = repo.findByUserCursor("user-1", START, END, 20, cursor);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).expenseId());
    }

    @Test
    void findByUserCursor_noResults_returnsEmptyList() {
        when(jdbc.query(eq(ExpenseRepository.FIND_BY_USER_CURSOR_SQL), any(RowMapper.class),
            eq("user-1"), eq(START), eq(END), eq(20)))
            .thenReturn(List.of());

        var result = repo.findByUserCursor("user-1", START, END, 20, null);

        assertTrue(result.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ExpenseEntity anEntity(Long id) {
        return new ExpenseEntity(id, LocalDate.of(2026, 1, 15), 1L,
            new BigDecimal("50.00"), "Lunch", "Food", "Restaurants", "user-1");
    }
}
