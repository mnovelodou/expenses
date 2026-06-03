package com.novelosoftware.expenses.services;

import com.novelosoftware.expenses.dto.CursorPageResponse;
import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.SubCategory;
import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.ExpenseValidationException;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.InvalidCursorException;
import com.novelosoftware.expenses.mappers.CategoryMapper;
import com.novelosoftware.expenses.repositories.ExpenseRepository;
import com.novelosoftware.expenses.util.ExpenseCursor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceListByUserTest {

    private static final String USER = "user-1";

    @Mock
    ExpenseRepository repo;

    @Mock
    AccountService accountService;

    @InjectMocks
    ExpenseService service;

    // -------------------------------------------------------------------------
    // Date defaults
    // -------------------------------------------------------------------------

    @Test
    void listByUser_bothDatesAbsent_defaultsToLastCalendarMonth() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDate expectedStart = lastMonth.atDay(1);
        LocalDate expectedEnd = lastMonth.atEndOfMonth();

        when(repo.findByUserCursor(eq(USER), eq(expectedStart), eq(expectedEnd), eq(20), isNull()))
            .thenReturn(List.of());

        CursorPageResponse<Expense> result = service.listByUser(USER, null, null, null, null);

        assertNotNull(result);
        assertEquals(0, result.content().size());
        assertNull(result.nextCursor());
    }

    @Test
    void listByUser_onlyStartDateProvided_endDateDerivedAsStartPlusOneMonth() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate expectedEnd = start.plusMonths(1);

        when(repo.findByUserCursor(eq(USER), eq(start), eq(expectedEnd), eq(20), isNull()))
            .thenReturn(List.of());

        service.listByUser(USER, start, null, null, null);
    }

    @Test
    void listByUser_onlyEndDateProvided_startDateDerivedAsEndMinusOneMonth() {
        LocalDate end = LocalDate.of(2026, 5, 1);
        LocalDate expectedStart = end.minusMonths(1);

        when(repo.findByUserCursor(eq(USER), eq(expectedStart), eq(end), eq(20), isNull()))
            .thenReturn(List.of());

        service.listByUser(USER, null, end, null, null);
    }

    // -------------------------------------------------------------------------
    // Date validation
    // -------------------------------------------------------------------------

    @Test
    void listByUser_endDateBeforeStartDate_throws400() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 4, 1);

        assertThrows(ExpenseValidationException.class,
            () -> service.listByUser(USER, start, end, null, null));
    }

    @Test
    void listByUser_rangeExceeds3CalendarMonths_throws400() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 5, 1); // 4 months

        assertThrows(ExpenseValidationException.class,
            () -> service.listByUser(USER, start, end, null, null));
    }

    @Test
    void listByUser_rangeExactly3CalendarMonths_isAllowed() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 4, 1); // exactly 3 months

        when(repo.findByUserCursor(eq(USER), eq(start), eq(end), anyInt(), isNull()))
            .thenReturn(List.of());

        assertDoesNotThrow(() -> service.listByUser(USER, start, end, null, null));
    }

    // -------------------------------------------------------------------------
    // Limit validation
    // -------------------------------------------------------------------------

    @Test
    void listByUser_limitAboveCap_throws400() {
        assertThrows(ExpenseValidationException.class,
            () -> service.listByUser(USER, null, null, 101, null));
    }

    @Test
    void listByUser_limitZero_throws400() {
        assertThrows(ExpenseValidationException.class,
            () -> service.listByUser(USER, null, null, 0, null));
    }

    @Test
    void listByUser_limitNegative_throws400() {
        assertThrows(ExpenseValidationException.class,
            () -> service.listByUser(USER, null, null, -5, null));
    }

    // -------------------------------------------------------------------------
    // Cursor validation
    // -------------------------------------------------------------------------

    @Test
    void listByUser_cursorDateOutsideRange_throwsInvalidCursorException() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 5, 1);
        // cursor encodes a date outside the [start, end] window
        String cursor = ExpenseCursor.encode(LocalDate.of(2026, 3, 1), 10L);

        assertThrows(InvalidCursorException.class,
            () -> service.listByUser(USER, start, end, null, cursor));
    }

    // -------------------------------------------------------------------------
    // nextCursor
    // -------------------------------------------------------------------------

    @Test
    void listByUser_resultsEqualLimit_nextCursorIsPresent() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 5, 1);
        int limit = 2;

        List<ExpenseEntity> entities = List.of(anEntity(10L, LocalDate.of(2026, 4, 20)),
                                               anEntity(9L,  LocalDate.of(2026, 4, 15)));

        when(repo.findByUserCursor(eq(USER), eq(start), eq(end), eq(limit), isNull()))
            .thenReturn(entities);

        CursorPageResponse<Expense> result = service.listByUser(USER, start, end, limit, null);

        assertNotNull(result.nextCursor());
    }

    @Test
    void listByUser_resultsLessThanLimit_nextCursorIsNull() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 5, 1);
        int limit = 20;

        when(repo.findByUserCursor(eq(USER), eq(start), eq(end), eq(limit), isNull()))
            .thenReturn(List.of(anEntity(5L, LocalDate.of(2026, 4, 10))));

        CursorPageResponse<Expense> result = service.listByUser(USER, start, end, limit, null);

        assertNull(result.nextCursor());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private ExpenseEntity anEntity(Long id, LocalDate date) {
        return new ExpenseEntity(id, date, 1L, new BigDecimal("50.00"), "Lunch",
            CategoryMapper.getCategory(SubCategory.RESTAURANT).name(),
            SubCategory.RESTAURANT.name(), USER);
    }
}
