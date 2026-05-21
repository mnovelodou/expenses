package com.novelosoftware.expenses.repositories;

import com.novelosoftware.expenses.entities.AccountEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {

    private final JdbcTemplate jdbc;

    public AccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<AccountEntity> mapper = (rs, row) -> new AccountEntity(
        rs.getLong("account_id"),
        rs.getString("name"),
        rs.getString("account_type"),
        rs.getString("currency"),
        rs.getBigDecimal("initial_amount"),
        rs.getBigDecimal("current_amount"),
        rs.getObject("created_at", java.time.OffsetDateTime.class),
        rs.getObject("updated_at", java.time.OffsetDateTime.class),
        rs.getString("created_by")
    );

    public List<AccountEntity> findAll() {
        return jdbc.query("SELECT * FROM accounts", mapper);
    }

    public Optional<AccountEntity> findById(Long id) {
        var results = jdbc.query("SELECT * FROM accounts WHERE account_id = ?", mapper, id);
        return results.stream().findFirst();
    }

    public List<AccountEntity> findByUser(String userId) {
        return jdbc.query("SELECT * FROM accounts WHERE created_by = ?", mapper, userId);
    }

    public AccountEntity create(AccountEntity entity) {
        var sql = """
            INSERT INTO accounts (name, account_type, currency, initial_amount, current_amount, created_by)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING *
            """;
        return jdbc.queryForObject(sql, mapper,
            entity.name(), entity.accountType(), entity.currency(),
            entity.initialAmount(), entity.initialAmount(), entity.createdBy());
    }

    public Optional<AccountEntity> update(Long id, AccountEntity entity) {
        var sql = """
            UPDATE accounts SET name = ?, account_type = ?, currency = ?,
            current_amount = ?, updated_at = CURRENT_TIMESTAMP
            WHERE account_id = ?
            RETURNING *
            """;
        var results = jdbc.query(sql, mapper,
            entity.name(), entity.accountType(), entity.currency(),
            entity.currentAmount(), id);
        return results.stream().findFirst();
    }

    public boolean delete(Long id) {
        return jdbc.update("DELETE FROM accounts WHERE account_id = ?", id) > 0;
    }
}
