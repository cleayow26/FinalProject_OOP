package com.neonpulse.backend.run.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SubmitRunRequest(
        @NotBlank @Size(max = 100) String clientRunId,
        @Size(max = 100) String level,
        @PositiveOrZero @Max(10000) int perfect,
        @PositiveOrZero @Max(10000) int good,
        @PositiveOrZero @Max(10000) int miss,
        @Positive @Max(3600000) long durationMs,
        boolean completed) {}
