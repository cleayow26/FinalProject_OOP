package com.neonpulse.client.backend;

import com.neonpulse.client.model.LeaderboardRow;
import com.neonpulse.client.model.PlayerAccount;
import com.neonpulse.client.model.RunResult;
import com.neonpulse.client.model.RunTally;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryBackendGatewayTest {

    private static final String LEVEL = "neon-pulse-level-1";

    private static RunTally tally(String clientRunId) {
        return new RunTally(clientRunId, LEVEL, 3, 2, 1, 60_000L, true);
    }

    @Test
    void replayReturnsIdenticalResult() {
        BackendGateway gateway = new InMemoryBackendGateway();
        PlayerAccount account = gateway.register("player_one", "example-password");
        RunTally tally = tally("run-0001");

        RunResult first = gateway.submitRun(account, tally);
        RunResult replay = gateway.submitRun(account, tally);

        assertEquals(first, replay, "a replay must keep the original id, score, and fields");
    }

    @Test
    void crossAccountSameClientRunIdCreatesTwoRuns() {
        BackendGateway gateway = new InMemoryBackendGateway();
        PlayerAccount firstAccount = gateway.register("player_one", "example-password");
        PlayerAccount secondAccount = gateway.register("player_two", "another-password");
        RunTally tally = tally("run-0001");

        RunResult first = gateway.submitRun(firstAccount, tally);
        RunResult second = gateway.submitRun(secondAccount, tally);

        assertNotEquals(first.id(), second.id(), "each account must get its own run id");
        List<LeaderboardRow> board = gateway.getLeaderboard(10);
        assertEquals(2, board.size(), "both runs must appear on the leaderboard");
        assertTrue(board.stream().anyMatch(row -> row.username().equals("player_one")));
        assertTrue(board.stream().anyMatch(row -> row.username().equals("player_two")));
    }

    @Test
    void leaderboardOrdersEqualScoresDeterministically() {
        BackendGateway gateway = new InMemoryBackendGateway();
        PlayerAccount secondAccount = gateway.register("player_two", "another-password");
        PlayerAccount firstAccount = gateway.register("player_one", "example-password");
        gateway.submitRun(secondAccount, tally("run-0002"));
        gateway.submitRun(firstAccount, tally("run-0001"));

        List<LeaderboardRow> board = gateway.getLeaderboard(10);

        assertEquals("player_one", board.get(0).username(), "ties break by username ascending");
        assertEquals("player_two", board.get(1).username());
        assertEquals(1, board.get(0).rank(), "equal scores share rank 1");
        assertEquals(1, board.get(1).rank());
    }

    @Test
    void scoreFormulaForThreeTwoOneCompletedIsNineHundred() {
        BackendGateway gateway = new InMemoryBackendGateway();
        PlayerAccount account = gateway.register("player_one", "example-password");

        RunResult result = gateway.submitRun(account, tally("run-0001"));

        assertEquals(900, result.score(), "3*100 + 2*50 + 500 = 900");
    }

    @Test
    void invalidCredentialsAndInvalidTallyRaiseInvalid() {
        BackendGateway gateway = new InMemoryBackendGateway();
        PlayerAccount account = gateway.register("player_one", "example-password");

        BackendException blankCredentials = assertThrows(
                BackendException.class,
                () -> gateway.register("   ", "example-password"));
        BackendException blankRunId = assertThrows(
                BackendException.class,
                () -> gateway.submitRun(account, tally("   ")));

        assertEquals(BackendException.Kind.INVALID, blankCredentials.kind());
        assertEquals(BackendException.Kind.INVALID, blankRunId.kind());
    }

    @Test
    void wrongPasswordRaisesUnauthorized() {
        BackendGateway gateway = new InMemoryBackendGateway();
        gateway.register("player_one", "example-password");

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.login("player_one", "wrong-password"));

        assertEquals(BackendException.Kind.UNAUTHORIZED, error.kind());
    }

    @Test
    void submitWithUnregisteredAccountRaisesUnauthorized() {
        BackendGateway gateway = new InMemoryBackendGateway();
        PlayerAccount unknown = new PlayerAccount("ghost", "token-ghost");

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.submitRun(unknown, tally("run-0001")));

        assertEquals(BackendException.Kind.UNAUTHORIZED, error.kind());
    }
}
