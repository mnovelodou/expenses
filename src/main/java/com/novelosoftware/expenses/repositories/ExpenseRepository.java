package com.novelosoftware.expenses.repositories;

import com.novelosoftware.expenses.entities.ExpenseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Provides CRUD and filtered query operations for the expenses table using JDBC.
 * All finder methods accept a date range and support pagination via limit/offset.
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
     * Returns a page of expenses for a given user within a date range.
     *
     * @param userId    the user ID to filter by
     * @param startDate inclusive start of the date range
     * @param endDate   inclusive end of the date range
     * @param limit     maximum number of results to return
     * @param offset    number of results to skip
     * @return list of expense entities for the requested page
     */
    public List<ExpenseEntity> findByUser(String userId, LocalDate startDate, LocalDate endDate,
                                          int limit, int offset) {
        var sql = """
            SELECT * FROM expenses
            WHERE created_by = ? AND expense_date BETWEEN ? AND ?
            ORDER BY expense_date DESC
            LIMIT ? OFFSET ?
            """;
        return jdbc.query(sql, MAPPER, userId, startDate, endDate, limit, offset);
    }

    /**
     * Counts the total expenses for a given user within a date range.
     *
     * @param userId    the user ID to filter by
     * @param startDate inclusive start of the date range
     * @param endDate   inclusive end of the date range
     * @return total count of matching expenses
     */
    public long countByUser(String userId, LocalDate startDate, LocalDate endDate) {
        var count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM expenses WHERE created_by = ? AND expense_date BETWEEN ? AND ?",
            Long.class, userId, startDate, endDate);
        return count != null ? count : 0L;
    }

    /**
     * Returns a page of expenses for a given user and account within a date range.
     * Both {@code userId} and {@code accountId} are required to prevent one user
     * from reading another user's expenses by guessing an account ID.
     *
     * @param userId    the owner of the expenses
     * @param accountId the account ID to filter by
     * @param startDate inclusive start of the date range
     * @param endDate   inclusive end of the date range
     * @param limit     maximum number of results to return
     * @param offset    number of results to skip
     * @return list of expense entities for the requested page
     */
    public List<ExpenseEntity> findByAccount(String userId, Long accountId, LocalDate startDate, LocalDate endDate,
                                              int limit, int offset) {
        var sql = """
            SELECT * FROM expenses
            WHERE created_by = ? AND account_id = ? AND expense_date BETWEEN ? AND ?
            ORDER BY expense_date DESC
            LIMIT ? OFFSET ?
            """;
        return jdbc.query(sql, MAPPER, userId, accountId, startDate, endDate, limit, offset);
    }

    public Optional<ExpenseEntity> get(Long expenseId) {
        return jdbc.query(GET_SQL, MAPPER, expenseId).stream().findFirst();
    }

    /**
     * Counts the total expenses for a given user and account within a date range.
     * Both {@code userId} and {@code accountId} are required to prevent one user
     * from reading another user's expenses by guessing an account ID.
     *
     * @param userId    the owner of the expenses
     * @param accountId the account ID to filter by
     * @param startDate inclusive start of the date range
     * @param endDate   inclusive end of the date range
     * @return total count of matching expenses
     */
    public long countByAccount(String userId, Long accountId, LocalDate startDate, LocalDate endDate) {
        var count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM expenses WHERE created_by = ? AND account_id = ? AND expense_date BETWEEN ? AND ?",
            Long.class, userId, accountId, startDate, endDate);
        return count != null ? count : 0L;
    }

    /**
     * Returns a page of expenses for a given user and category within a date range.
     *
     * @param userId    the user ID to filter by
     * @param category  the category to filter by
     * @param startDate inclusive start of the date range
     * @param endDate   inclusive end of the date range
     * @param limit     maximum number of results to return
     * @param offset    number of results to skip
     * @return list of expense entities for the requested page
     */
    public List<ExpenseEntity> findByCategory(String userId, String category,
                                               LocalDate startDate, LocalDate endDate,
                                               int limit, int offset) {
        var sql = """
            SELECT * FROM expenses
            WHERE created_by = ? AND category = ? AND expense_date BETWEEN ? AND ?
            ORDER BY expense_date DESC
            LIMIT ? OFFSET ?
            """;
        return jdbc.query(sql, MAPPER, userId, category, startDate, endDate, limit, offset);
    }

    /**
     * Counts the total expenses for a given user and category within a date range.
     *
     * @param userId    the user ID to filter by
     * @param category  the category to filter by
     * @param startDate inclusive start of the date range
     * @param endDate   inclusive end of the date range
     * @return total count of matching expenses
     */
    public long countByCategory(String userId, String category, LocalDate startDate, LocalDate endDate) {
        var count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM expenses WHERE created_by = ? AND category = ? AND expense_date BETWEEN ? AND ?",
            Long.class, userId, category, startDate, endDate);
        return count != null ? count : 0L;
    }

    /**
     * Returns a page of expenses for a given user and subcategory within a date range.
     *
     * @param userId      the user ID to filter by
     * @param subcategory the subcategory to filter by
     * @param startDate   inclusive start of the date range
     * @param endDate     inclusive end of the date range
     * @param limit       maximum number of results to return
     * @param offset      number of results to skip
     * @return list of expense entities for the requested page
     */
    public List<ExpenseEntity> findBySubcategory(String userId, String subcategory,
                                                  LocalDate startDate, LocalDate endDate,
                                                  int limit, int offset) {
        var sql = """
            SELECT * FROM expenses
            WHERE created_by = ? AND subcategory = ? AND expense_date BETWEEN ? AND ?
            ORDER BY expense_date DESC
            LIMIT ? OFFSET ?
            """;
        return jdbc.query(sql, MAPPER, userId, subcategory, startDate, endDate, limit, offset);
    }

    /**
     * Counts the total expenses for a given user and subcategory within a date range.
     *
     * @param userId      the user ID to filter by
     * @param subcategory the subcategory to filter by
     * @param startDate   inclusive start of the date range
     * @param endDate     inclusive end of the date range
     * @return total count of matching expenses
     */
    public long countBySubcategory(String userId, String subcategory, LocalDate startDate, LocalDate endDate) {
        var count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM expenses WHERE created_by = ? AND subcategory = ? AND expense_date BETWEEN ? AND ?",
            Long.class, userId, subcategory, startDate, endDate);
        return count != null ? count : 0L;
    }
}
