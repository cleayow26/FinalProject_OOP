CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE levels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE runs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    level_id BIGINT NOT NULL REFERENCES levels(id),
    client_run_id VARCHAR(100) NOT NULL,
    perfect INT NOT NULL CHECK (perfect >= 0),
    good INT NOT NULL CHECK (good >= 0),
    miss INT NOT NULL CHECK (miss >= 0),
    duration_ms BIGINT NOT NULL CHECK (duration_ms > 0),
    completed BOOLEAN NOT NULL,
    score INT NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_run_client UNIQUE (user_id, client_run_id)
);

INSERT INTO levels (name) VALUES ('neon-pulse-level-1');
