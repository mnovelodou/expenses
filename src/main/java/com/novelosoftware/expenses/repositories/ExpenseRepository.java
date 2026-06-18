package com.novelosoftware.expenses.repositories;

import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.util.ExpenseCursor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
        rs.getBigDecimal("transaction_amount"),
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
            INSERT INTO expenses (expense_date, account_id, amount, transaction_amount, description, category, subcategory, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """;
        return jdbc.queryForObject(sql, MAPPER,
            entity.expenseDate(), entity.accountId(), entity.amount(), entity.transactionAmount(),
            entity.description(), entity.category(), entity.subcategory(), entity.createdBy());
    }

    /**
     * Inserts multiple expenses in a single statement and returns the persisted entities
     * including generated IDs. The returned list is ordered by {@code expense_id ASC},
     * which matches insertion order because the ID is assigned by a monotonically increasing sequence.
     *
     * <p>This method does not open its own transaction. Callers that need atomicity (all-or-nothing)
     * must invoke it within a {@code @Transactional} scope (see {@code ExpenseService.bulkCreate}).
     *
     * @param entities the expenses to insert (must be non-empty)
     * @return the inserted expense entities with generated IDs
     */
    public List<ExpenseEntity> bulkInsert(List<ExpenseEntity> entities) {
        var sql = new StringBuilder(
            "WITH inserted AS (INSERT INTO expenses " +
            "(expense_date, account_id, amount, transaction_amount, description, category, subcategory, created_by) VALUES ");

        var params = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("(?, ?, ?, ?, ?, ?, ?, ?)");
            ExpenseEntity e = entities.get(i);
            params.add(e.expenseDate());
            params.add(e.accountId());
            params.add(e.amount());
            params.add(e.transactionAmount());
            params.add(e.description());
            params.add(e.category());
            params.add(e.subcategory());
            params.add(e.createdBy());
        }
        // ORDER BY expense_id ASC makes the returned order deterministic and matches insertion order
        // because expense_id is assigned by a monotonically increasing sequence.
        sql.append(" RETURNING *) SELECT * FROM inserted ORDER BY expense_id ASC");

        return jdbc.query(sql.toString(), MAPPER, params.toArray());
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
            SET expense_date = ?, account_id = ?, amount = ?, transaction_amount = ?,
                description = ?, category = ?, subcategory = ?
            WHERE expense_id = ?
            RETURNING *
            """;
        var results = jdbc.query(sql, MAPPER,
            entity.expenseDate(), entity.accountId(), entity.amount(), entity.transactionAmount(),
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
     * @param transactionAmount optional exact-match filter on the original transaction amount
     * @param limit       maximum number of results to return
     * @param cursor      optional cursor from the previous page; {@code null} for the first page
     * @return list of expense entities for the requested page
     */
    public List<ExpenseEntity> findByFiltersCursor(
            String userId, LocalDate startDate, LocalDate endDate,
            String category, String subcategory, Long accountId,
            BigDecimal transactionAmount,
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
        if (transactionAmount != null) {
            sql.append("AND transaction_amount = ? ");
            params.add(transactionAmount);
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

    /**
     * Sums the amounts of all expenses for an account on or after the given date.
     *
     * <p>The {@code idx_expenses_account_date (account_id, expense_date)} index bounds
     * the scan so the aggregation stays cheap as the expenses table grows.
     *
     * @param accountId the account whose expenses to aggregate
     * @param since     inclusive lower bound on {@code expense_date}
     * @return the sum of matching expense amounts, or zero if none match
     */
    public BigDecimal sumByAccountSince(Long accountId, LocalDate since) {
        var sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE account_id = ? AND expense_date >= ?";
        var result = jdbc.queryForObject(sql, BigDecimal.class, accountId, since);
        return result != null ? result : BigDecimal.ZERO;
    }
}
