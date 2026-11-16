package com.neonpulse.backend.run.dto;

import java.time.Instant;

public record RunResultResponse(
        Long id,
        String clientRunId,
        String level,
        int perfect,
        int good,
        int miss,
        long durationMs,
        boolean completed,
        int score,
        Instant submittedAt) {}
