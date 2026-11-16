package com.neonpulse.backend.run;

import com.neonpulse.backend.run.dto.RunResultResponse;
import com.neonpulse.backend.run.dto.SubmitRunRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runs")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    @PostMapping
    public ResponseEntity<RunResultResponse> submit(
            Authentication authentication, @Valid @RequestBody SubmitRunRequest request) {
        RunService.SubmissionResult result = runService.submit(authentication.getName(), request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.response());
    }

    @GetMapping("/me")
    public List<RunResultResponse> myRuns(Authentication authentication) {
        return runService.findMyRuns(authentication.getName());
    }
}
