package com.neonpulse.backend.run;

import com.neonpulse.backend.run.dto.RunResultResponse;
import com.neonpulse.backend.run.dto.SubmitRunRequest;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class RunRepository {

    private static final String SELECT = "SELECT r.id, r.client_run_id, l.name AS level, r.perfect, r.good, r.miss, "
            + "r.duration_ms, r.completed, r.score, r.submitted_at "
            + "FROM runs r JOIN levels l ON l.id = r.level_id";

    private static final RowMapper<RunResultResponse> MAPPER = (rs, rowNum) -> new RunResultResponse(
            rs.getLong("id"),
            rs.getString("client_run_id"),
            rs.getString("level"),
            rs.getInt("perfect"),
            rs.getInt("good"),
            rs.getInt("miss"),
            rs.getLong("duration_ms"),
            rs.getBoolean("completed"),
            rs.getInt("score"),
            rs.getTimestamp("submitted_at").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public RunRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Long> findLevelIdByName(String name) {
        return jdbcTemplate
                .query("SELECT id FROM levels WHERE name = ?", (rs, rowNum) -> rs.getLong("id"), name)
                .stream()
                .findFirst();
    }

    public RunResultResponse insert(Long userId, String levelName, SubmitRunRequest request, int score) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO runs (user_id, level_id, client_run_id, perfect, good, miss, "
                                    + "duration_ms, completed, score) "
                                    + "VALUES (?, (SELECT id FROM levels WHERE name = ?), ?, ?, ?, ?, ?, ?, ?)",
                            new String[] {"id"});
                    statement.setLong(1, userId);
                    statement.setString(2, levelName);
                    statement.setString(3, request.clientRunId());
                    statement.setInt(4, request.perfect());
                    statement.setInt(5, request.good());
                    statement.setInt(6, request.miss());
                    statement.setLong(7, request.durationMs());
                    statement.setBoolean(8, request.completed());
                    statement.setInt(9, score);
                    return statement;
                },
                keyHolder);
        return jdbcTemplate.queryForObject(
                SELECT + " WHERE r.id = ?", MAPPER, keyHolder.getKey().longValue());
    }

    public Optional<RunResultResponse> findByUserAndClientRunId(Long userId, String clientRunId) {
        return jdbcTemplate
                .query(SELECT + " WHERE r.user_id = ? AND r.client_run_id = ?", MAPPER, userId, clientRunId)
                .stream()
                .findFirst();
    }

    public List<RunResultResponse> findAllByUser(Long userId) {
        return jdbcTemplate.query(
                SELECT + " WHERE r.user_id = ? ORDER BY r.submitted_at DESC, r.id DESC", MAPPER, userId);
    }
}
