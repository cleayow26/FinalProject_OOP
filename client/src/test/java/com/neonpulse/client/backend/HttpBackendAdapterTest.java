package com.neonpulse.client.backend;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.neonpulse.client.model.LeaderboardRow;
import com.neonpulse.client.model.PlayerAccount;
import com.neonpulse.client.model.RunResult;
import com.neonpulse.client.model.RunTally;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpBackendAdapterTest {
    private static final String LEVEL = "neon-pulse-level-1";
    private static final String RUN_JSON = """
            {"id":1,"clientRunId":"run-0001","level":"neon-pulse-level-1",
             "perfect":3,"good":2,"miss":1,"durationMs":60000,"completed":true,
             "score":900,"submittedAt":"2026-11-16T12:00:00Z"}
            """;
    private static final String RUN_JSON_MISSING_SCORE = """
            {"id":1,"clientRunId":"run-0001","level":"neon-pulse-level-1",
             "perfect":3,"good":2,"miss":1,"durationMs":60000,"completed":true,
             "submittedAt":"2026-11-16T12:00:00Z"}
            """;
    private static final String LEADERBOARD_JSON = """
            [{"rank":1,"username":"player_one","score":900,"submittedAt":"2026-11-16T12:00:00Z"},
             {"rank":2,"username":"player_two","score":750,"submittedAt":"2026-11-16T12:05:00Z"}]
            """;

    private HttpServer server;
    private Map<String, HttpHandler> routes;
    private TokenStore tokens;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        routes = new ConcurrentHashMap<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            HttpHandler handler = routes.get(exchange.getRequestURI().getPath());
            if (handler == null) {
                send(exchange, 404, "{}");
            } else {
                handler.handle(exchange);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        tokens = new InMemoryTokenStore();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void submitRunSendsExactJsonFieldsAndAccountToken() {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> contentType = new AtomicReference<>();
        routes.put("/api/runs", exchange -> {
            body.set(readBody(exchange));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            send(exchange, 201, RUN_JSON);
        });
        tokens.saveToken("store-token");
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        RunResult result = gateway.submitRun(
                new PlayerAccount("player_one", "account-token"),
                new RunTally("run-0001", LEVEL, 3, 2, 1, 60_000L, true));

        assertEquals("Bearer account-token", authorization.get(), "submit must use the account token");
        assertEquals("application/json", contentType.get(), "submit must send a JSON content type");
        JsonValue sent = new JsonReader().parse(body.get());
        assertEquals(Set.of("clientRunId", "level", "perfect", "good", "miss", "durationMs", "completed"),
                fieldNames(sent), "submit must send exactly the seven run fields");
        assertEquals("run-0001", sent.getString("clientRunId"));
        assertEquals(LEVEL, sent.getString("level"));
        assertEquals(3, sent.getInt("perfect"));
        assertEquals(2, sent.getInt("good"));
        assertEquals(1, sent.getInt("miss"));
        assertEquals(60_000L, sent.getLong("durationMs"));
        assertTrue(sent.getBoolean("completed"));
        assertEquals(900, result.score(), "the response must be parsed into a RunResult");
        assertEquals("run-0001", result.clientRunId());
    }

    @Test
    void submitFallsBackToStoredTokenWhenAccountTokenIsBlank() {
        AtomicReference<String> authorization = new AtomicReference<>();
        routes.put("/api/runs", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            send(exchange, 201, RUN_JSON);
        });
        tokens.saveToken("store-token");
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        gateway.submitRun(new PlayerAccount("player_one", "  "), tally("run-0001"));

        assertEquals("Bearer store-token", authorization.get());
    }

    @Test
    void submitAccepts200Response() {
        routes.put("/api/runs", exchange -> send(exchange, 200, RUN_JSON));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        RunResult result = gateway.submitRun(new PlayerAccount("player_one", "account-token"), tally("run-0001"));

        assertEquals(900, result.score(), "200 is the replay status and must parse like 201");
    }

    @Test
    void submitOmitsLevelWhenBlank() {
        AtomicReference<String> body = new AtomicReference<>();
        routes.put("/api/runs", exchange -> {
            body.set(readBody(exchange));
            send(exchange, 201, RUN_JSON);
        });
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        gateway.submitRun(new PlayerAccount("player_one", "account-token"),
                new RunTally("run-0001", "   ", 3, 2, 1, 60_000L, true));

        JsonValue sent = new JsonReader().parse(body.get());
        assertFalse(fieldNames(sent).contains("level"), "blank level is optional and must be omitted");
        assertEquals(6, fieldNames(sent).size());
    }

    @Test
    void registerSavesTokenFromResponse() {
        routes.put("/api/auth/register", exchange ->
                send(exchange, 201, "{\"token\":\"saved-token\",\"username\":\"player_one\"}"));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        PlayerAccount account = gateway.register("player_one", "example-password");

        assertEquals("player_one", account.username());
        assertEquals("saved-token", account.token());
        assertEquals("saved-token", tokens.getToken(), "register must persist the returned token");
    }

    @Test
    void loginSavesTokenFromResponse() {
        routes.put("/api/auth/login", exchange ->
                send(exchange, 200, "{\"token\":\"login-token\",\"username\":\"player_one\"}"));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        PlayerAccount account = gateway.login("player_one", "example-password");

        assertEquals("login-token", account.token());
        assertEquals("login-token", tokens.getToken(), "login must persist the returned token");
    }

    @Test
    void authEndpointsSendNoAuthorizationHeader() {
        AtomicReference<String> registerAuth = new AtomicReference<>();
        AtomicReference<String> loginAuth = new AtomicReference<>();
        routes.put("/api/auth/register", exchange -> {
            registerAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            send(exchange, 201, "{\"token\":\"saved-token\",\"username\":\"player_one\"}");
        });
        routes.put("/api/auth/login", exchange -> {
            loginAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            send(exchange, 200, "{\"token\":\"login-token\",\"username\":\"player_one\"}");
        });
        tokens.saveToken("stored-token");
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        gateway.register("player_one", "example-password");
        gateway.login("player_one", "example-password");

        assertNull(registerAuth.get(), "register must not send Authorization");
        assertNull(loginAuth.get(), "login must not send Authorization");
    }

    @Test
    void registerEscapesQuotesInUsername() {
        AtomicReference<String> body = new AtomicReference<>();
        routes.put("/api/auth/register", exchange -> {
            body.set(readBody(exchange));
            send(exchange, 201, "{\"token\":\"saved-token\",\"username\":\"player_one\"}");
        });
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);
        String username = "player\"one";

        gateway.register(username, "example-password");

        assertEquals(username, new JsonReader().parse(body.get()).getString("username"),
                "the server must receive valid JSON with the quote preserved");
    }

    @Test
    void leaderboardSendsBearerHeader() {
        AtomicReference<String> authorization = new AtomicReference<>();
        routes.put("/api/leaderboard", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            send(exchange, 200, "[]");
        });
        tokens.saveToken("stored-token");
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        gateway.getLeaderboard(10);

        assertEquals("Bearer stored-token", authorization.get());
    }

    @Test
    void leaderboardParsesRows() {
        routes.put("/api/leaderboard", exchange -> send(exchange, 200, LEADERBOARD_JSON));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        List<LeaderboardRow> rows = gateway.getLeaderboard(10);

        assertEquals(List.of(
                new LeaderboardRow(1, "player_one", 900, "2026-11-16T12:00:00Z"),
                new LeaderboardRow(2, "player_two", 750, "2026-11-16T12:05:00Z")), rows);
    }

    @Test
    void limitIsValidatedBeforeSending() {
        AtomicInteger requests = new AtomicInteger();
        routes.put("/api/leaderboard", exchange -> {
            requests.incrementAndGet();
            send(exchange, 200, "[]");
        });
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        BackendException tooSmall = assertThrows(BackendException.class, () -> gateway.getLeaderboard(0));
        BackendException tooLarge = assertThrows(BackendException.class, () -> gateway.getLeaderboard(101));

        assertEquals(BackendException.Kind.INVALID, tooSmall.kind());
        assertEquals(BackendException.Kind.INVALID, tooLarge.kind());
        assertEquals(0, requests.get(), "invalid limits must be rejected before any request is sent");
    }

    @Test
    void loginWrongCredentialsMaps401ToUnauthorized() {
        routes.put("/api/auth/login", exchange ->
                send(exchange, 401, "{\"timestamp\":\"2026-11-16T12:00:00Z\",\"status\":401,"
                        + "\"error\":\"Unauthorized\",\"message\":\"Bad credentials\"}"));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.login("player_one", "wrong-password"));

        assertEquals(BackendException.Kind.UNAUTHORIZED, error.kind());
        assertTrue(error.getMessage().contains("Bad credentials"));
        assertFalse(error.getMessage().contains("401"), "status codes must not leak to callers");
        assertFalse(error.getMessage().contains("{"), "raw JSON must not leak to callers");
    }

    @Test
    void duplicateRegisterMaps409ToConflict() {
        routes.put("/api/auth/register", exchange ->
                send(exchange, 409, "{\"status\":409,\"error\":\"Conflict\","
                        + "\"message\":\"Username already exists\"}"));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.register("player_one", "example-password"));

        assertEquals(BackendException.Kind.CONFLICT, error.kind());
        assertTrue(error.getMessage().contains("Username already exists"));
    }

    @Test
    void badRequestMapsToInvalid() {
        routes.put("/api/auth/register", exchange ->
                send(exchange, 400, "{\"status\":400,\"message\":\"Validation failed\","
                        + "\"fieldErrors\":{\"username\":\"must not be blank\"}}"));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.register("", "example-password"));

        assertEquals(BackendException.Kind.INVALID, error.kind());
        assertTrue(error.getMessage().contains("must not be blank"));
    }

    @Test
    void serverErrorMapsToServer() {
        routes.put("/api/auth/register", exchange ->
                send(exchange, 503, "{\"status\":503,\"message\":\"Service unavailable\"}"));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.register("player_one", "example-password"));

        assertEquals(BackendException.Kind.SERVER, error.kind());
        assertTrue(error.getMessage().contains("Service unavailable"));
    }

    @Test
    void unexpectedRedirectMapsToTransport() {
        routes.put("/api/auth/register", exchange -> send(exchange, 302, ""));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.register("player_one", "example-password"));

        assertEquals(BackendException.Kind.TRANSPORT, error.kind());
    }

    @Test
    void connectionFailureMapsToTransport() throws IOException {
        int port;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            port = socket.getLocalPort();
        }
        BackendGateway gateway = new HttpBackendAdapter("http://127.0.0.1:" + port, tokens);

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.getLeaderboard(10));

        assertEquals(BackendException.Kind.TRANSPORT, error.kind());
    }

    @Test
    void malformedSuccessBodyMapsToInvalid() {
        routes.put("/api/leaderboard", exchange -> send(exchange, 200, "{\"rank\":"));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.getLeaderboard(10));

        assertEquals(BackendException.Kind.INVALID, error.kind());
    }

    @Test
    void missingFieldInSuccessBodyMapsToInvalid() {
        routes.put("/api/runs", exchange -> send(exchange, 201, RUN_JSON_MISSING_SCORE));
        BackendGateway gateway = new HttpBackendAdapter(baseUrl, tokens);

        BackendException error = assertThrows(
                BackendException.class,
                () -> gateway.submitRun(new PlayerAccount("player_one", "account-token"), tally("run-0001")));

        assertEquals(BackendException.Kind.INVALID, error.kind());
        assertEquals("Malformed response", error.getMessage());
    }

    private static RunTally tally(String clientRunId) {
        return new RunTally(clientRunId, LEVEL, 3, 2, 1, 60_000L, true);
    }

    private static Set<String> fieldNames(JsonValue object) {
        Set<String> names = new HashSet<>();
        for (JsonValue child = object.child; child != null; child = child.next) {
            names.add(child.name());
        }
        return names;
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
