package com.novelosoftware.expenses.util;

import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.ExpenseValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DateWindowResolver}.
 * Tested in isolation so the resolution and validation logic can be verified
 * independently of any specific query type.
 */
class DateWindowResolverTest {

    // Fixed clock: "now" = 2026-06-02 → last month = May 2026
    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-06-02T00:00:00Z"), ZoneOffset.UTC);

    private static final LocalDate MAY_FIRST  = LocalDate.of(2026, 5, 1);
    private static final LocalDate MAY_LAST   = LocalDate.of(2026, 5, 31);

    // -------------------------------------------------------------------------
    // Defaulting
    // -------------------------------------------------------------------------

    @Test
    void bothAbsent_defaultsToLastCalendarMonth() {
        DateWindow window = DateWindowResolver.resolve(null, null, CLOCK);
        assertEquals(MAY_FIRST, window.startDate());
        assertEquals(MAY_LAST, window.endDate());
    }

    @Test
    void onlyStartProvided_endDerivedAsStartPlusOneMonth() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        DateWindow window = DateWindowResolver.resolve(start, null, CLOCK);
        assertEquals(start, window.startDate());
        assertEquals(LocalDate.of(2026, 5, 1), window.endDate());
    }

    @Test
    void onlyEndProvided_startDerivedAsEndMinusOneMonth() {
        LocalDate end = LocalDate.of(2026, 5, 1);
        DateWindow window = DateWindowResolver.resolve(null, end, CLOCK);
        assertEquals(LocalDate.of(2026, 4, 1), window.startDate());
        assertEquals(end, window.endDate());
    }

    @Test
    void bothProvided_usedAsIs() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 4, 1);
        DateWindow window = DateWindowResolver.resolve(start, end, CLOCK);
        assertEquals(start, window.startDate());
        assertEquals(end, window.endDate());
    }

    // -------------------------------------------------------------------------
    // Validation — invalid ranges
    // -------------------------------------------------------------------------

    @Test
    void endBeforeStart_throws400() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 4, 1);
        assertThrows(ExpenseValidationException.class,
            () -> DateWindowResolver.resolve(start, end, CLOCK));
    }

    @ParameterizedTest(name = "rangeExceeds3Months_{0}")
    @MethodSource("rangesExceeding3Months")
    void rangeExceeds3CalendarMonths_throws400(String label, LocalDate start, LocalDate end) {
        assertThrows(ExpenseValidationException.class,
            () -> DateWindowResolver.resolve(start, end, CLOCK));
    }

    static Stream<Arguments> rangesExceeding3Months() {
        return Stream.of(
            Arguments.of("4_months", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 1)),
            Arguments.of("6_months", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 7, 1)),
            Arguments.of("1_year",   LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1))
        );
    }

    // -------------------------------------------------------------------------
    // Validation — boundary: exactly 3 months is allowed
    // -------------------------------------------------------------------------

    @Test
    void rangeExactly3CalendarMonths_isAllowed() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 4, 1); // exactly 3 months
        assertDoesNotThrow(() -> DateWindowResolver.resolve(start, end, CLOCK));
    }

    @Test
    void sameDayRange_isAllowed() {
        LocalDate date = LocalDate.of(2026, 5, 15);
        DateWindow window = DateWindowResolver.resolve(date, date, CLOCK);
        assertEquals(date, window.startDate());
        assertEquals(date, window.endDate());
    }
}
