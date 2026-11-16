package com.neonpulse.backend.user;

import java.time.Instant;

public record User(Long id, String username, String passwordHash, Instant createdAt) {}
