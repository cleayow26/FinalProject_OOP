# NEON PULSE API

Base URL: `http://localhost:8080`

All endpoints except registration and login require `Authorization: Bearer <token>`. Both `POST /api/auth/register` and `POST /api/auth/login` return a token, valid for 1440 minutes. `GET /actuator/health` is public and needs no token.

## Endpoints

`POST /api/auth/register`

```json
{"username":"player_one","password":"example-password"}
```

Returns `201` with `{"token":"eyJ...","username":"player_one"}`. A duplicate username returns `409`.

`POST /api/auth/login`

```json
{"username":"player_one","password":"example-password"}
```

Returns `200` with `{"token":"eyJ...","username":"player_one"}`. Wrong credentials return `401`.

`GET /api/me`

Returns `200` with `{"username":"player_one"}`.

`POST /api/runs`

```json
{
  "clientRunId":"run-0001",
  "level":"neon-pulse-level-1",
  "perfect":3,
  "good":2,
  "miss":1,
  "durationMs":60000,
  "completed":true
}
```

`level` is optional and defaults to `neon-pulse-level-1`. Returns `201` with the stored run:

```json
{
  "id":1,
  "clientRunId":"run-0001",
  "level":"neon-pulse-level-1",
  "perfect":3,
  "good":2,
  "miss":1,
  "durationMs":60000,
  "completed":true,
  "score":900,
  "submittedAt":"2026-11-16T12:00:00Z"
}
```

`GET /api/runs/me` returns `200` with an array of the same run objects, newest first.

`GET /api/leaderboard?limit=10` returns `200` with an array ranked by score. Equal scores share a rank; `submittedAt` only orders display. The board is per run, so one player may hold multiple slots:

```json
[
  {"rank":1,"username":"player_one","score":900,"submittedAt":"2026-11-16T12:00:00Z"},
  {"rank":2,"username":"player_two","score":750,"submittedAt":"2026-11-16T12:05:00Z"}
]
```

`limit` must be between 1 and 100 and defaults to 10.

## Scoring

`score = perfect * 100 + good * 50 + (completed ? 500 : 0)`

`miss` and `durationMs` are stored but do not affect the score.

## Idempotency

`clientRunId` is unique per user. Submitting the same `clientRunId` again returns `200` with the original run id and score, and inserts no new row. Generate a fresh `clientRunId` for every new attempt.

## When the client calls the backend

1. First launch: register the player, store the token.
2. Later launches: log in, then call `/api/me` to confirm the stored token.
3. A run finishes, including retries after a network failure: submit with the same `clientRunId`.
4. The player opens the history or leaderboard screen: call `/api/runs/me` and `/api/leaderboard`.

## Errors

Every error has the same shape:

```json
{
  "timestamp":"2026-11-16T12:00:00Z",
  "status":400,
  "error":"Bad Request",
  "message":"Validation failed",
  "fieldErrors":{"username":"must not be blank"}
}
```

`fieldErrors` may be omitted when empty. Status codes: `400` invalid input, `401` missing or bad token and bad credentials, `404` unknown level, `409` duplicate username, `405` wrong method, `415` wrong content type, `500` unexpected server error.
