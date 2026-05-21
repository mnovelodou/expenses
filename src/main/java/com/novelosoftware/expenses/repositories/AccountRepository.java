package com.novelosoftware.expenses.repositories;

import com.novelosoftware.expenses.entities.AccountEntity;
import com.novelosoftware.expenses.enums.AccountType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

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

    private final RowMapper<AccountEntity> mapper = (rs, row) -> new AccountEntity(
        rs.getLong("account_id"),
        rs.getString("name"),
        AccountType.valueOf(rs.getString("account_type")),
        rs.getString("currency"),
        rs.getBigDecimal("initial_amount"),
        rs.getBigDecimal("current_amount"),
        rs.getObject("created_at", java.time.OffsetDateTime.class),
        rs.getObject("updated_at", java.time.OffsetDateTime.class),
        rs.getString("created_by")
    );

    /**
     * Returns all accounts.
     *
     * @return list of all account entities
     */
    public List<AccountEntity> findAll() {
        return jdbc.query("SELECT * FROM accounts", mapper);
    }

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
     * Returns all accounts owned by a given user.
     *
     * @param userId the user ID to filter by
     * @return list of account entities belonging to the user
     */
    public List<AccountEntity> findByUser(String userId) {
        return jdbc.query("SELECT * FROM accounts WHERE created_by = ?", mapper, userId);
    }

    /**
     * Inserts a new account and returns the persisted entity including generated fields.
     *
     * @param entity the account to insert
     * @return the inserted account entity with generated ID and timestamps
     */
    public AccountEntity create(AccountEntity entity) {
        var sql = """
            INSERT INTO accounts (name, account_type, currency, initial_amount, current_amount, created_by)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING *
            """;
        return jdbc.queryForObject(sql, mapper,
            entity.name(), entity.accountType().name(), entity.currency(),
            entity.initialAmount(), entity.initialAmount(), entity.createdBy());
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
            current_amount = ?, updated_at = CURRENT_TIMESTAMP
            WHERE account_id = ?
            RETURNING *
            """;
        var results = jdbc.query(sql, mapper,
            entity.name(), entity.accountType().name(), entity.currency(),
            entity.currentAmount(), id);
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
