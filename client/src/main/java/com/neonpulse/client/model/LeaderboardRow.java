package com.neonpulse.client.model;

public record LeaderboardRow(long rank, String username, int score, String submittedAt) {
}
