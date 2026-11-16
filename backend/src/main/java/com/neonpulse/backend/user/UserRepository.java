package com.neonpulse.backend.user;

import java.sql.PreparedStatement;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final String SELECT = "SELECT id, username, password_hash, created_at FROM users";

    private static final RowMapper<User> MAPPER = (rs, rowNum) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByUsername(String username) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM users WHERE username = ?)", Boolean.class, username);
        return Boolean.TRUE.equals(exists);
    }

    public Optional<User> findByUsername(String username) {
        return jdbcTemplate.query(SELECT + " WHERE username = ?", MAPPER, username).stream()
                .findFirst();
    }

    public User insert(String username, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO users (username, password_hash) VALUES (?, ?)", new String[] {"id"});
                    statement.setString(1, username);
                    statement.setString(2, passwordHash);
                    return statement;
                },
                keyHolder);
        return jdbcTemplate.queryForObject(
                SELECT + " WHERE id = ?", MAPPER, keyHolder.getKey().longValue());
    }
}
