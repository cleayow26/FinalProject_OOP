package com.neonpulse.client.model;

public record RunTally(
        String clientRunId,
        String level,
        int perfect,
        int good,
        int miss,
        long durationMs,
        boolean completed
) {
}
