package com.neonpulse.backend.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@Testcontainers
@SpringBootTest
class RunIntegrationTest {

    private static final String VALID_RUN_BODY = "{\"clientRunId\":\"run-e2e\",\"level\":\"neon-pulse-level-1\","
            + "\"perfect\":3,\"good\":2,\"miss\":1,\"durationMs\":60000,\"completed\":true}";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        jdbcTemplate.update("TRUNCATE runs, users RESTART IDENTITY CASCADE");
    }

    @Test
    void submitRunReturnsCreatedWithComputedScore() throws Exception {
        String token = registerAndLogin("run_user", "run-password");

        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientRunId").value("run-e2e"))
                .andExpect(jsonPath("$.level").value("neon-pulse-level-1"))
                .andExpect(jsonPath("$.score").value(900));
    }

    @Test
    void replayedRunReturnsOriginalResultWithoutInserting() throws Exception {
        String token = registerAndLogin("replay_user", "replay-password");

        MvcResult first = mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isCreated())
                .andReturn();
        long firstId = objectMapper
                .readTree(first.getResponse().getContentAsString())
                .get("id")
                .asLong();
        Long countBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM runs", Long.class);

        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId))
                .andExpect(jsonPath("$.score").value(900));

        Long countAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM runs", Long.class);
        assertThat(countAfter).isEqualTo(countBefore);
    }

    @Test
    void myRunsReturnsTheSubmittedRun() throws Exception {
        String token = registerAndLogin("history_user", "history-password");
        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/runs/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientRunId").value("run-e2e"))
                .andExpect(jsonPath("$[0].score").value(900))
                .andExpect(jsonPath("$[0].level").value("neon-pulse-level-1"));
    }

    @Test
    void leaderboardRanksTheOnlyPlayerFirst() throws Exception {
        String token = registerAndLogin("leader_user", "leader-password");
        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/leaderboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].username").value("leader_user"))
                .andExpect(jsonPath("$[0].score").value(900));
    }

    @Test
    void leaderboardReordersByScore() throws Exception {
        String lowToken = registerAndLogin("low_user", "low-password");
        String highToken = registerAndLogin("high_user", "high-password");
        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + lowToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + highToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRunId\":\"run-high\",\"perfect\":10,\"good\":0,\"miss\":0,"
                                + "\"durationMs\":30000,\"completed\":true}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/leaderboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + highToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].username").value("high_user"))
                .andExpect(jsonPath("$[0].score").value(1500))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].username").value("low_user"))
                .andExpect(jsonPath("$[1].score").value(900));

        mockMvc.perform(get("/api/runs/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + lowToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientRunId").value("run-e2e"))
                .andExpect(jsonPath("$[0].level").value("neon-pulse-level-1"))
                .andExpect(jsonPath("$[0].perfect").value(3))
                .andExpect(jsonPath("$[0].good").value(2))
                .andExpect(jsonPath("$[0].miss").value(1))
                .andExpect(jsonPath("$[0].durationMs").value(60000))
                .andExpect(jsonPath("$[0].completed").value(true))
                .andExpect(jsonPath("$[0].score").value(900));
    }

    @Test
    void submitWithoutTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitWithNegativePerfectReturnsBadRequest() throws Exception {
        String token = registerAndLogin("invalid_user", "invalid-password");

        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRunId\":\"run-negative\",\"perfect\":-1,\"good\":0,\"miss\":0,"
                                + "\"durationMs\":1000,\"completed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.perfect").exists());
    }

    @Test
    void myRunsReturnsNewestSubmissionFirst() throws Exception {
        String token = registerAndLogin("order_user", "order-password");
        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRunId\":\"run-first\",\"perfect\":1,\"good\":0,\"miss\":0,"
                                + "\"durationMs\":1000,\"completed\":false}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRunId\":\"run-second\",\"perfect\":2,\"good\":0,\"miss\":0,"
                                + "\"durationMs\":2000,\"completed\":false}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/runs/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].clientRunId").value("run-second"))
                .andExpect(jsonPath("$[1].clientRunId").value("run-first"));
    }

    @Test
    void sameClientRunIdIsScopedPerUser() throws Exception {
        String firstToken = registerAndLogin("scope_a_user", "scope-password");
        String secondToken = registerAndLogin("scope_b_user", "scope-password");
        String sharedBody = "{\"clientRunId\":\"shared-run-id\",\"perfect\":2,\"good\":0,\"miss\":0,"
                + "\"durationMs\":1000,\"completed\":false}";

        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sharedBody))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sharedBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/runs/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientRunId").value("shared-run-id"));
        mockMvc.perform(get("/api/runs/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].clientRunId").value("shared-run-id"));
        mockMvc.perform(get("/api/leaderboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void equalScoresShareRankOne() throws Exception {
        String firstToken = registerAndLogin("tie_a_user", "tie-password");
        String secondToken = registerAndLogin("tie_b_user", "tie-password");

        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/leaderboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].score").value(900))
                .andExpect(jsonPath("$[1].rank").value(1))
                .andExpect(jsonPath("$[1].score").value(900));
    }

    @Test
    void replayWithChangedLevelReturnsOriginalResult() throws Exception {
        String token = registerAndLogin("replay_level_user", "replay-password");
        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RUN_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRunId\":\"run-e2e\",\"level\":\"does-not-exist\","
                                + "\"perfect\":99,\"good\":0,\"miss\":0,\"durationMs\":1000,\"completed\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("neon-pulse-level-1"))
                .andExpect(jsonPath("$.perfect").value(3))
                .andExpect(jsonPath("$.score").value(900));
    }

    @Test
    void submitWithOverLimitPerfectReturnsBadRequest() throws Exception {
        String token = registerAndLogin("bound_user", "bound-password");

        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRunId\":\"run-over\",\"perfect\":10001,\"good\":0,\"miss\":0,"
                                + "\"durationMs\":1000,\"completed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.perfect").exists());
    }

    @Test
    void leaderboardRejectsLimitZero() throws Exception {
        String token = registerAndLogin("limit_zero_user", "limit-password");

        mockMvc.perform(get("/api/leaderboard?limit=0").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leaderboardRejectsLimitAboveHundred() throws Exception {
        String token = registerAndLogin("limit_high_user", "limit-password");

        mockMvc.perform(get("/api/leaderboard?limit=101").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void leaderboardRejectsNonNumericLimit() throws Exception {
        String token = registerAndLogin("limit_text_user", "limit-password");

        mockMvc.perform(get("/api/leaderboard?limit=abc").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitWithUnknownLevelReturnsNotFound() throws Exception {
        String token = registerAndLogin("level_user", "level-password");

        mockMvc.perform(post("/api/runs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientRunId\":\"run-unknown\",\"level\":\"does-not-exist\","
                                + "\"perfect\":1,\"good\":0,\"miss\":0,\"durationMs\":1000,\"completed\":true}"))
                .andExpect(status().isNotFound());
    }

    private String registerAndLogin(String username, String password) throws Exception {
        String credentials = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper
                .readTree(login.getResponse().getContentAsString())
                .get("token")
                .asText();
    }
}
