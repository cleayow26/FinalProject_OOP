# NEON PULSE

A 2D rhythm platformer for Object-Oriented Programming final project.

## Layout

- `client/` - Game client. Copied into the LibGDX game core
- `backend/` - Spring Boot API, PostgreSQL, Flyway migrations.
- `docs/` - Code documentation

## Documentation

- [Bakckend API](docs/API.md)

## Quick start

1. Copy `backend/.env.example` to `backend/.env` and replace each placeholder. The JWT secret must be at least 32 characters.
2. From `backend/`, run `docker compose up --build`.
3. Check `http://localhost:8080/actuator/health`.

## Tests

- Backend: from `backend/`, run `./gradlew test`. Testcontainers needs a Docker daemon to mock a PostgreSQL server.
- Client: from `client/`, run `./gradlew test`.
