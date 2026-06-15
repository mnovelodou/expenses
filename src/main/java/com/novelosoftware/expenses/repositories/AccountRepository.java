package com.novelosoftware.expenses.repositories;

import com.novelosoftware.expenses.dto.AccountType;
import com.novelosoftware.expenses.entities.AccountEntity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Provides CRUD operations for the accounts table using JDBC.
 */
@Repository
public class AccountRepository {

    private final JdbcTemplate jdbc;

    /**
     * @param jdbc the JdbcTemplate used to execute queries
     */
    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<AccountEntity> mapper = (rs, row) -> new AccountEntity(
        rs.getLong("account_id"),
        rs.getString("name"),
        AccountType.valueOf(rs.getString("account_type")),
        rs.getString("currency"),
        rs.getBigDecimal("initial_amount"),
        rs.getBigDecimal("current_amount"),
        rs.getObject("created_at", java.time.OffsetDateTime.class),
        rs.getObject("updated_at", java.time.OffsetDateTime.class),
        rs.getString("created_by"),
        rs.getObject("period_start", LocalDate.class));

    /**
     * Finds a single account by its ID.
     *
     * @param id the account ID
     * @return an Optional containing the entity if found, empty otherwise
     */
    public Optional<AccountEntity> findById(Long id) {
        var results = jdbc.query("SELECT * FROM accounts WHERE account_id = ?", mapper, id);
        return results.stream().findFirst();
    }

    /**
     * Returns a page of accounts owned by a given user, ordered by account_id.
     *
     * @param userId the user ID to filter by
     * @param limit  the maximum number of results to return
     * @param offset the number of results to skip
     * @return list of account entities for the requested page
     */
    public List<AccountEntity> findByUser(String userId, int limit, int offset) {
        return jdbc.query(
            "SELECT * FROM accounts WHERE created_by = ? ORDER BY account_id LIMIT ? OFFSET ?",
            mapper, userId, limit, offset);
    }

    /**
     * Counts the total number of accounts owned by a given user.
     *
     * @param userId the user ID to filter by
     * @return total count of accounts belonging to the user
     */
    public long countByUser(String userId) {
        var count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM accounts WHERE created_by = ?", Long.class, userId);
        return count != null ? count : 0L;
    }

    /**
     * Inserts a new account and returns the persisted entity including generated fields.
     *
     * @param entity the account to insert
     * @return the inserted account entity with generated ID and timestamps
     */
    public AccountEntity create(AccountEntity entity) {
        var sql = """
            INSERT INTO accounts (name, account_type, currency, initial_amount, current_amount, created_by, period_start)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING *
            """;
        return jdbc.queryForObject(sql, mapper,
            entity.name(), entity.accountType().name(), entity.currency(),
            entity.initialAmount(), entity.currentAmount(), entity.createdBy(), entity.periodStart());
    }

    /**
     * Updates an existing account and returns the updated entity.
     *
     * @param id     the ID of the account to update
     * @param entity the entity carrying the updated field values
     * @return an Optional containing the updated entity if found, empty otherwise
     */
    public Optional<AccountEntity> update(Long id, AccountEntity entity) {
        var sql = """
            UPDATE accounts SET name = ?, account_type = ?, currency = ?,
            initial_amount = ?, current_amount = ?, period_start = ?, updated_at = CURRENT_TIMESTAMP
            WHERE account_id = ?
            RETURNING *
            """;
        var results = jdbc.query(sql, mapper,
            entity.name(), entity.accountType().name(), entity.currency(),
            entity.initialAmount(), entity.currentAmount(), entity.periodStart(), id);
        return results.stream().findFirst();
    }

    /**
     * Deletes an account by its ID.
     *
     * @param id the ID of the account to delete
     * @return true if a row was deleted, false if no account with that ID existed
     */
    public boolean delete(Long id) {
        return jdbc.update("DELETE FROM accounts WHERE account_id = ?", id) > 0;
    }
}
