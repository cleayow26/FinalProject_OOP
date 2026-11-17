package com.neonpulse.client.backend;

import com.neonpulse.client.model.LeaderboardRow;
import com.neonpulse.client.model.PlayerAccount;
import com.neonpulse.client.model.RunResult;
import com.neonpulse.client.model.RunTally;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryBackendGateway implements BackendGateway {
    private static final String SUBMITTED_AT = "2026-11-16T12:00:00Z";

    private final Map<String, String> passwords = new ConcurrentHashMap<>();
    private final Map<String, PlayerAccount> accounts = new ConcurrentHashMap<>();
    private final Map<String, RunResult> runsByAccountAndId = new ConcurrentHashMap<>();
    private final Map<Long, String> ownerByRunId = new ConcurrentHashMap<>();
    private final AtomicLong nextRunId = new AtomicLong(1);
    private final Object runsLock = new Object();

    @Override
    public PlayerAccount register(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            throw new BackendException(BackendException.Kind.INVALID, "Username and password must not be blank");
        }
        if (passwords.putIfAbsent(username, password) != null) {
            throw new BackendException(BackendException.Kind.CONFLICT, "Username already registered");
        }
        PlayerAccount account = new PlayerAccount(username, "token-" + username);
        accounts.put(username, account);
        return account;
    }

    @Override
    public PlayerAccount login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            throw new BackendException(BackendException.Kind.INVALID, "Username and password must not be blank");
        }
        String stored = passwords.get(username);
        if (stored == null || !stored.equals(password)) {
            throw new BackendException(BackendException.Kind.UNAUTHORIZED, "Invalid username or password");
        }
        return accounts.get(username);
    }

    @Override
    public RunResult submitRun(PlayerAccount account, RunTally tally) {
        if (account == null || isBlank(account.username()) || isBlank(account.token())
                || !accounts.containsKey(account.username())) {
            throw new BackendException(BackendException.Kind.UNAUTHORIZED, "A valid account is required");
        }
        if (tally == null || isBlank(tally.clientRunId())
                || tally.perfect() < 0 || tally.good() < 0 || tally.miss() < 0
                || tally.durationMs() < 0) {
            throw new BackendException(BackendException.Kind.INVALID, "Run data is invalid");
        }

        String key = account.username() + '\u0000' + tally.clientRunId();
        RunResult existing = runsByAccountAndId.get(key);
        if (existing != null) {
            return existing;
        }

        int score = tally.perfect() * 100 + tally.good() * 50 + (tally.completed() ? 500 : 0);
        long id = nextRunId.getAndIncrement();
        RunResult result = new RunResult(
                id,
                tally.clientRunId(),
                tally.level(),
                tally.perfect(),
                tally.good(),
                tally.miss(),
                tally.durationMs(),
                tally.completed(),
                score,
                SUBMITTED_AT);
        synchronized (runsLock) {
            runsByAccountAndId.put(key, result);
            ownerByRunId.put(id, account.username());
        }
        return result;
    }

    @Override
    public List<LeaderboardRow> getLeaderboard(int limit) {
        if (limit < 1 || limit > 100) {
            throw new BackendException(BackendException.Kind.INVALID, "Limit must be between 1 and 100");
        }

        List<RunResult> sorted;
        Map<Long, String> owners;
        synchronized (runsLock) {
            sorted = new ArrayList<>(runsByAccountAndId.values());
            owners = Map.copyOf(ownerByRunId);
        }

        Comparator<RunResult> ordering = Comparator.comparingInt(RunResult::score).reversed()
                .thenComparing(result -> owners.getOrDefault(result.id(), ""))
                .thenComparing(RunResult::clientRunId)
                .thenComparingLong(RunResult::id);
        sorted.sort(ordering);

        List<LeaderboardRow> rows = new ArrayList<>();
        int previousScore = Integer.MIN_VALUE;
        long rank = 0;
        for (RunResult result : sorted) {
            if (rank == 0 || result.score() != previousScore) {
                rank = rows.size() + 1L;
                previousScore = result.score();
            }
            rows.add(new LeaderboardRow(
                    rank,
                    owners.getOrDefault(result.id(), ""),
                    result.score(),
                    result.submittedAt()));
            if (rows.size() == limit) {
                break;
            }
        }
        return rows;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
