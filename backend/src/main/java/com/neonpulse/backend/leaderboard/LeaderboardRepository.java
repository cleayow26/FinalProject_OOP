package com.neonpulse.backend.leaderboard;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class LeaderboardRepository {

    private static final String SELECT = "SELECT RANK() OVER (ORDER BY r.score DESC) AS rank_value, "
            + "u.username, r.score, r.submitted_at "
            + "FROM runs r JOIN users u ON u.id = r.user_id "
            + "ORDER BY rank_value, r.submitted_at ASC LIMIT ?";

    private static final RowMapper<LeaderboardEntry> MAPPER = (rs, rowNum) -> new LeaderboardEntry(
            rs.getLong("rank_value"),
            rs.getString("username"),
            rs.getInt("score"),
            rs.getTimestamp("submitted_at").toInstant());

    private final JdbcTemplate jdbcTemplate;

    public LeaderboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LeaderboardEntry> findTop(int limit) {
        return jdbcTemplate.query(SELECT, MAPPER, limit);
    }
}
