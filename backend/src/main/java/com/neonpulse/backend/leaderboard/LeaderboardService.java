package com.neonpulse.backend.leaderboard;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardService {

    private final LeaderboardRepository leaderboardRepository;

    public LeaderboardService(LeaderboardRepository leaderboardRepository) {
        this.leaderboardRepository = leaderboardRepository;
    }

    public List<LeaderboardEntry> top(int limit) {
        return leaderboardRepository.findTop(limit);
    }
}
