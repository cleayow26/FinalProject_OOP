package com.neonpulse.client.backend;

import com.neonpulse.client.model.LeaderboardRow;
import com.neonpulse.client.model.PlayerAccount;
import com.neonpulse.client.model.RunResult;
import com.neonpulse.client.model.RunTally;

import java.util.List;

public interface BackendGateway {
    PlayerAccount register(String username, String password);

    PlayerAccount login(String username, String password);

    RunResult submitRun(PlayerAccount account, RunTally tally);

    List<LeaderboardRow> getLeaderboard(int limit);
}
