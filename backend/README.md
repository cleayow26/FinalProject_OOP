# Backend

Spring Boot 4.1 backend for NEON PULSE. PostgreSQL with Flyway migrations, JdbcTemplate for data access.

Run the commands below from `backend/`.

## Setup

1. Copy `.env.example` to `.env` and replace the placeholder values for `DB_USER`, `DB_PASSWORD`, `DB_NAME`, and `JWT_SECRET`. The JWT secret must be at least 32 characters.
2. Run `docker compose up --build`.
3. The API listens on `http://localhost:8080`; health is at `/actuator/health`.

## Tests

Run `./gradlew test`.

Testcontainers requires a running Docker daemon.

The endpoint contract is in [API.md](API.md).
