package com.novelosoftware.expenses.util;

import java.time.LocalDate;

/**
 * A resolved, validated date window with an inclusive start and end date.
 */
public record DateWindow(LocalDate startDate, LocalDate endDate) {}
