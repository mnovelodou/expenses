package com.novelosoftware.expenses.repositories;

import com.novelosoftware.expenses.dto.CategoryFilter;
import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.util.ExpenseCursor;
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
    // findByFiltersCursor — no filters
    // -------------------------------------------------------------------------

    @Test
    void findByFiltersCursor_noFilters_noCursor_returnsResults() {
        when(jdbc.query(anyString(), any(RowMapper.class), eq("user-1"), eq(START), eq(END), eq(20)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByFiltersCursor("user-1", START, END, null, null, 20, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).expenseId());
    }

    @Test
    void findByFiltersCursor_noFilters_withCursor_appendsCursorPredicate() {
        LocalDate cursorDate = LocalDate.of(2026, 1, 15);
        long cursorId = 5L;
        var cursor = new ExpenseCursor.DecodedCursor(cursorDate, cursorId);

        when(jdbc.query(anyString(), any(RowMapper.class),
            eq("user-1"), eq(START), eq(END),
            eq(cursorDate), eq(cursorDate), eq(cursorId),
            eq(20)))
            .thenReturn(List.of(anEntity(3L)));

        var result = repo.findByFiltersCursor("user-1", START, END, null, null, 20, cursor);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).expenseId());
    }

    // -------------------------------------------------------------------------
    // findByFiltersCursor — single filters
    // -------------------------------------------------------------------------

    @Test
    void findByFiltersCursor_categoryFilter_passedAsParam() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            eq("user-1"), eq(START), eq(END), eq("Food"), eq(20)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByFiltersCursor("user-1", START, END,
            CategoryFilter.ofCategory("Food"), null, 20, null);

        assertEquals(1, result.size());
    }

    @Test
    void findByFiltersCursor_subcategoryFilter_passedAsParam() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            eq("user-1"), eq(START), eq(END), eq("Groceries"), eq(20)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByFiltersCursor("user-1", START, END,
            CategoryFilter.ofSubcategory("Groceries"), null, 20, null);

        assertEquals(1, result.size());
    }

    @Test
    void findByFiltersCursor_accountIdFilter_passedAsParam() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            eq("user-1"), eq(START), eq(END), eq(3L), eq(20)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByFiltersCursor("user-1", START, END, null, 3L, 20, null);

        assertEquals(1, result.size());
    }

    // -------------------------------------------------------------------------
    // findByFiltersCursor — combined filters
    // -------------------------------------------------------------------------

    @Test
    void findByFiltersCursor_categoryAndAccountId_bothPassedAsParams() {
        when(jdbc.query(anyString(), any(RowMapper.class),
            eq("user-1"), eq(START), eq(END), eq("Food"), eq(3L), eq(20)))
            .thenReturn(List.of(anEntity(1L)));

        var result = repo.findByFiltersCursor("user-1", START, END,
            CategoryFilter.ofCategory("Food"), 3L, 20, null);

        assertEquals(1, result.size());
    }

    @Test
    void findByFiltersCursor_noResults_returnsEmptyList() {
        when(jdbc.query(anyString(), any(RowMapper.class), any()))
            .thenReturn(List.of());

        var result = repo.findByFiltersCursor("user-1", START, END, null, null, 20, null);

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
