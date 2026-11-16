package com.neonpulse.backend.run;

import com.neonpulse.backend.common.ApiException;
import com.neonpulse.backend.run.dto.RunResultResponse;
import com.neonpulse.backend.run.dto.SubmitRunRequest;
import com.neonpulse.backend.user.User;
import com.neonpulse.backend.user.UserRepository;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RunService {

    private static final String DEFAULT_LEVEL = "neon-pulse-level-1";

    public record SubmissionResult(RunResultResponse response, boolean created) {}

    private final RunRepository runRepository;
    private final UserRepository userRepository;

    public RunService(RunRepository runRepository, UserRepository userRepository) {
        this.runRepository = runRepository;
        this.userRepository = userRepository;
    }

    public SubmissionResult submit(String username, SubmitRunRequest request) {
        User user = requireUser(username);
        var existing = runRepository.findByUserAndClientRunId(user.id(), request.clientRunId());
        if (existing.isPresent()) {
            return new SubmissionResult(existing.get(), false);
        }

        String levelName = request.level() == null || request.level().isBlank() ? DEFAULT_LEVEL : request.level();
        if (runRepository.findLevelIdByName(levelName).isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Unknown level: " + levelName);
        }

        int score = ScorePolicy.compute(
                request.perfect(), request.good(), request.miss(), request.durationMs(), request.completed());
        RunResultResponse response;
        try {
            response = runRepository.insert(user.id(), levelName, request, score);
        } catch (DuplicateKeyException ex) {
            return runRepository
                    .findByUserAndClientRunId(user.id(), request.clientRunId())
                    .map(stored -> new SubmissionResult(stored, false))
                    .orElseThrow(() -> ex);
        }
        return new SubmissionResult(response, true);
    }

    public List<RunResultResponse> findMyRuns(String username) {
        return runRepository.findAllByUser(requireUser(username).id());
    }

    private User requireUser(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Unknown user"));
    }
}
