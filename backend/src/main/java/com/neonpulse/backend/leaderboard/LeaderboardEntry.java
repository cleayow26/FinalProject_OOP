package com.neonpulse.backend.leaderboard;

import java.time.Instant;

public record LeaderboardEntry(long rank, String username, int score, Instant submittedAt) {}
