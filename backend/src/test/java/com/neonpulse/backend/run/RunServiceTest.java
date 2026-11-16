package com.neonpulse.backend.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.neonpulse.backend.common.ApiException;
import com.neonpulse.backend.run.dto.RunResultResponse;
import com.neonpulse.backend.run.dto.SubmitRunRequest;
import com.neonpulse.backend.user.User;
import com.neonpulse.backend.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RunServiceTest {

    private static final User USER = new User(7L, "service_user", "hash", Instant.now());

    @Mock
    private RunRepository runRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RunService runService;

    @Test
    void unknownLevelThrowsNotFound() {
        SubmitRunRequest request = request("unknown-level");
        when(userRepository.findByUsername("service_user")).thenReturn(Optional.of(USER));
        when(runRepository.findLevelIdByName("unknown-level")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runService.submit("service_user", request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(runRepository, never()).insert(anyLong(), anyString(), any(), anyInt());
    }

    @Test
    void duplicateClientRunIdReturnsStoredResponseWithoutInserting() {
        SubmitRunRequest request = request(null);
        RunResultResponse stored = storedResponse(42L, 900);
        when(userRepository.findByUsername("service_user")).thenReturn(Optional.of(USER));
        when(runRepository.findByUserAndClientRunId(7L, "run-42")).thenReturn(Optional.of(stored));

        RunService.SubmissionResult result = runService.submit("service_user", request);

        assertThat(result.response()).isEqualTo(stored);
        assertThat(result.created()).isFalse();
        verify(runRepository, never()).insert(anyLong(), anyString(), any(), anyInt());
    }

    @Test
    void replayIgnoresChangedLevel() {
        SubmitRunRequest changed = new SubmitRunRequest("run-42", "does-not-exist", 1, 0, 0, 1000L, false);
        RunResultResponse stored = storedResponse(45L, 900);
        when(userRepository.findByUsername("service_user")).thenReturn(Optional.of(USER));
        when(runRepository.findByUserAndClientRunId(7L, "run-42")).thenReturn(Optional.of(stored));

        RunService.SubmissionResult result = runService.submit("service_user", changed);

        assertThat(result.response()).isEqualTo(stored);
        assertThat(result.created()).isFalse();
        verify(runRepository, never()).findLevelIdByName(anyString());
        verify(runRepository, never()).insert(anyLong(), anyString(), any(), anyInt());
    }

    @Test
    void duplicateKeyRaceReturnsStoredResponseAsReplay() {
        SubmitRunRequest request = request(null);
        RunResultResponse stored = storedResponse(44L, 900);
        when(userRepository.findByUsername("service_user")).thenReturn(Optional.of(USER));
        when(runRepository.findByUserAndClientRunId(7L, "run-42")).thenReturn(Optional.empty(), Optional.of(stored));
        when(runRepository.findLevelIdByName("neon-pulse-level-1")).thenReturn(Optional.of(1L));
        when(runRepository.insert(7L, "neon-pulse-level-1", request, 900))
                .thenThrow(new DuplicateKeyException("uq_run_client"));

        RunService.SubmissionResult result = runService.submit("service_user", request);

        assertThat(result.response()).isEqualTo(stored);
        assertThat(result.created()).isFalse();
    }

    @Test
    void duplicateKeyRaceRethrowsWhenStoredRunCannotBeRead() {
        SubmitRunRequest request = request(null);
        when(userRepository.findByUsername("service_user")).thenReturn(Optional.of(USER));
        when(runRepository.findByUserAndClientRunId(7L, "run-42")).thenReturn(Optional.empty());
        when(runRepository.findLevelIdByName("neon-pulse-level-1")).thenReturn(Optional.of(1L));
        when(runRepository.insert(7L, "neon-pulse-level-1", request, 900))
                .thenThrow(new DuplicateKeyException("uq_run_client"));

        assertThatThrownBy(() -> runService.submit("service_user", request)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void newRunComputesScoreAndInsertsOnce() {
        SubmitRunRequest request = request(null);
        RunResultResponse created = storedResponse(43L, 900);
        when(userRepository.findByUsername("service_user")).thenReturn(Optional.of(USER));
        when(runRepository.findLevelIdByName("neon-pulse-level-1")).thenReturn(Optional.of(1L));
        when(runRepository.findByUserAndClientRunId(7L, "run-42")).thenReturn(Optional.empty());
        when(runRepository.insert(7L, "neon-pulse-level-1", request, 900)).thenReturn(created);

        RunService.SubmissionResult result = runService.submit("service_user", request);

        assertThat(result.created()).isTrue();
        assertThat(result.response()).isEqualTo(created);
        verify(runRepository).insert(eq(7L), eq("neon-pulse-level-1"), eq(request), eq(900));
    }

    private static SubmitRunRequest request(String level) {
        return new SubmitRunRequest("run-42", level, 3, 2, 1, 60_000L, true);
    }

    private static RunResultResponse storedResponse(long id, int score) {
        return new RunResultResponse(id, "run-42", "neon-pulse-level-1", 3, 2, 1, 60_000L, true, score, Instant.now());
    }
}
