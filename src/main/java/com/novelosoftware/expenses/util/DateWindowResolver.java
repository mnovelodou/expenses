package com.novelosoftware.expenses.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;

import static com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.createValidationException;

/**
 * Resolves and validates an optional date window for expense list queries.
 *
 * <p>Defaulting rules (each parameter is resolved independently):
 * <ul>
 *   <li>Both absent → first day to last day of the previous calendar month.</li>
 *   <li>Only {@code start} provided → {@code end = start + 1 month}.</li>
 *   <li>Only {@code end} provided   → {@code start = end - 1 month}.</li>
 * </ul>
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code end} must not be before {@code start}.</li>
 *   <li>The resolved range must not exceed 3 calendar months.</li>
 * </ul>
 *
 * <p>Extracted as a standalone utility so it can be reused across different
 * query types (by-account, by-category, etc.) and tested in isolation.
 */
public final class DateWindowResolver {

    private DateWindowResolver() {}

    /**
     * Resolves and validates a date window from optional inputs.
     *
     * @param start optional start date; {@code null} to derive from {@code end} or use default
     * @param end   optional end date; {@code null} to derive from {@code start} or use default
     * @param clock clock to use when computing "now" (injectable for tests)
     * @return a validated {@link DateWindow}
     * @throws com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.ExpenseValidationException
     *         if the resolved range is invalid
     */
    public static DateWindow resolve(LocalDate start, LocalDate end, Clock clock) {
        LocalDate resolvedEnd = end;
        LocalDate resolvedStart = start;

        if (resolvedEnd == null && resolvedStart == null) {
            YearMonth lastMonth = YearMonth.now(clock).minusMonths(1);
            resolvedEnd = lastMonth.atEndOfMonth();
            resolvedStart = resolvedEnd.withDayOfMonth(1);
        } else if (resolvedStart == null) {
            resolvedStart = resolvedEnd.minusMonths(1);
        } else if (resolvedEnd == null) {
            resolvedEnd = resolvedStart.plusMonths(1);
        }

        validate(resolvedStart, resolvedEnd);
        return new DateWindow(resolvedStart, resolvedEnd);
    }

    private static void validate(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw createValidationException("end_date must not be before start_date");
        }
        if (start.plus(Period.ofMonths(3)).isBefore(end)) {
            throw createValidationException("Date range must not exceed 3 calendar months");
        }
    }
}
