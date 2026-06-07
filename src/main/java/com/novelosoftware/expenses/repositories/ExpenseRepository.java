package com.novelosoftware.expenses.repositories;

import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.util.ExpenseCursor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides CRUD and filtered query operations for the expenses table using JDBC.
 * <p>
 * All list queries require {@code created_by} and a date range — these are mandatory
 * so that {@code idx_expenses_user_date (created_by, expense_date DESC, expense_id DESC)}
 * can bound the scan. Optional filters (category, subcategory, accountId) are applied
 * as additional predicates on top of that bounded scan.
 */
@Repository
public class ExpenseRepository {

    static final String GET_SQL = "SELECT * FROM expenses WHERE expense_id = ?";

    static final RowMapper<ExpenseEntity> MAPPER = (rs, row) -> new ExpenseEntity(
        rs.getLong("expense_id"),
        rs.getObject("expense_date", LocalDate.class),
        rs.getLong("account_id"),
        rs.getBigDecimal("amount"),
        rs.getString("description"),
        rs.getString("category"),
        rs.getString("subcategory"),
        rs.getString("created_by")
    );

    private final JdbcTemplate jdbc;

    /**
     * @param jdbc the JdbcTemplate used to execute queries
     */
    public ExpenseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Inserts a new expense and returns the persisted entity including generated fields.
     *
     * @param entity the expense to insert
     * @return the inserted expense entity with generated ID
     */
    public ExpenseEntity create(ExpenseEntity entity) {
        var sql = """
            INSERT INTO expenses (expense_date, account_id, amount, description, category, subcategory, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """;
        return jdbc.queryForObject(sql, MAPPER,
            entity.expenseDate(), entity.accountId(), entity.amount(),
            entity.description(), entity.category(), entity.subcategory(), entity.createdBy());
    }

    /**
     * Inserts multiple expenses in a single statement and returns the persisted entities
     * including generated IDs. The returned list preserves insertion order.
     *
     * @param entities the expenses to insert (must be non-empty)
     * @return the inserted expense entities with generated IDs
     */
    public List<ExpenseEntity> bulkInsert(List<ExpenseEntity> entities) {
        var placeholders = "(" + "?, ".repeat(6) + "?)";
        var allPlaceholders = String.join(", ", java.util.Collections.nCopies(entities.size(), placeholders));
        var sql = """
            INSERT INTO expenses (expense_date, account_id, amount, description, category, subcategory, created_by)
            VALUES %s
            RETURNING *
            """.formatted(allPlaceholders);

        var params = new ArrayList<>();
        for (ExpenseEntity e : entities) {
            params.add(e.expenseDate());
            params.add(e.accountId());
            params.add(e.amount());
            params.add(e.description());
            params.add(e.category());
            params.add(e.subcategory());
            params.add(e.createdBy());
        }
        return jdbc.query(sql, MAPPER, params.toArray());
    }

    /**
     * Updates an existing expense and returns the updated entity.
     *
     * @param id     the ID of the expense to update
     * @param entity the entity carrying the updated field values
     * @return an Optional containing the updated entity if found, empty otherwise
     */
    public Optional<ExpenseEntity> update(Long id, ExpenseEntity entity) {
        var sql = """
            UPDATE expenses
            SET expense_date = ?, account_id = ?, amount = ?,
                description = ?, category = ?, subcategory = ?
            WHERE expense_id = ?
            RETURNING *
            """;
        var results = jdbc.query(sql, MAPPER,
            entity.expenseDate(), entity.accountId(), entity.amount(),
            entity.description(), entity.category(), entity.subcategory(), id);
        return results.stream().findFirst();
    }

    /**
     * Deletes an expense by its ID.
     *
     * @param id the ID of the expense to delete
     * @return true if a row was deleted, false if no expense with that ID existed
     */
    public boolean delete(Long id) {
        return jdbc.update("DELETE FROM expenses WHERE expense_id = ?", id) > 0;
    }

    // -------------------------------------------------------------------------
    // Finders
    // -------------------------------------------------------------------------

    /**
     * Returns a page of expenses for a given user within a date range, with optional
     * filters on category, subcategory, and accountId. Exactly one of category or
     * subcategory may be supplied; supplying both is a caller error and should be
     * rejected at the service layer before reaching this method.
     *
     * <p>Results are ordered by {@code expense_date DESC, expense_id DESC}.
     * When {@code cursor} is provided only expenses strictly older than the cursor
     * position are returned, enabling forward pagination.
     *
     * <p>The {@code idx_expenses_user_date} index bounds the scan via the mandatory
     * {@code userId} + date range predicates; the optional filters are applied on top.
     *
     * @param userId      the user whose expenses to fetch (mandatory)
     * @param startDate   inclusive start of the date range (mandatory)
     * @param endDate     inclusive end of the date range (mandatory)
     * @param category    optional category filter; mutually exclusive with subcategory
     * @param subcategory optional subcategory filter; mutually exclusive with category
     * @param accountId   optional account filter
     * @param limit       maximum number of results to return
     * @param cursor      optional cursor from the previous page; {@code null} for the first page
     * @return list of expense entities for the requested page
     */
    public List<ExpenseEntity> findByFiltersCursor(
            String userId, LocalDate startDate, LocalDate endDate,
            String category, String subcategory, Long accountId,
            int limit,
            ExpenseCursor.DecodedCursor cursor) {

        var sql = new StringBuilder("""
            SELECT * FROM expenses
            WHERE created_by = ? AND expense_date BETWEEN ? AND ?
            """);
        var params = new ArrayList<>();
        params.add(userId);
        params.add(startDate);
        params.add(endDate);

        if (category != null) {
            sql.append("AND category = ? ");
            params.add(category);
        }
        if (subcategory != null) {
            sql.append("AND subcategory = ? ");
            params.add(subcategory);
        }
        if (accountId != null) {
            sql.append("AND account_id = ? ");
            params.add(accountId);
        }
        if (cursor != null) {
            sql.append("AND (expense_date < ? OR (expense_date = ? AND expense_id < ?)) ");
            params.add(cursor.date());
            params.add(cursor.date());
            params.add(cursor.id());
        }

        sql.append("ORDER BY expense_date DESC, expense_id DESC LIMIT ?");
        params.add(limit);

        return jdbc.query(sql.toString(), MAPPER, params.toArray());
    }

    public Optional<ExpenseEntity> get(Long expenseId) {
        return jdbc.query(GET_SQL, MAPPER, expenseId).stream().findFirst();
    }
}
