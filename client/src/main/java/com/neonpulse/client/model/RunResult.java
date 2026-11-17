package com.neonpulse.client.model;

public record RunResult(
        long id,
        String clientRunId,
        String level,
        int perfect,
        int good,
        int miss,
        long durationMs,
        boolean completed,
        int score,
        String submittedAt
) {
}
