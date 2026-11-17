package com.neonpulse.client.backend;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.neonpulse.client.model.LeaderboardRow;
import com.neonpulse.client.model.PlayerAccount;
import com.neonpulse.client.model.RunResult;
import com.neonpulse.client.model.RunTally;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class HttpBackendAdapter implements BackendGateway {
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final String MALFORMED_RESPONSE = "Malformed response";

    private final String baseUrl;
    private final TokenStore tokenStore;
    private final HttpClient client;
    private final Duration requestTimeout;

    public HttpBackendAdapter(String baseUrl, TokenStore tokenStore) {
        this(baseUrl, tokenStore, DEFAULT_CONNECT_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
    }

    public HttpBackendAdapter(String baseUrl, TokenStore tokenStore, Duration connectTimeout, Duration requestTimeout) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.tokenStore = tokenStore;
        this.requestTimeout = requestTimeout;
        this.client = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }

    @Override
    public PlayerAccount register(String username, String password) {
        String json = requireSuccess(send("/api/auth/register", "POST", authBody(username, password), null), 201);
        JsonValue body = parseOrInvalid(json);
        PlayerAccount account = new PlayerAccount(requireString(body, "username"), requireString(body, "token"));
        tokenStore.saveToken(account.token());
        return account;
    }

    @Override
    public PlayerAccount login(String username, String password) {
        String json = requireSuccess(send("/api/auth/login", "POST", authBody(username, password), null), 200);
        JsonValue body = parseOrInvalid(json);
        PlayerAccount account = new PlayerAccount(requireString(body, "username"), requireString(body, "token"));
        tokenStore.saveToken(account.token());
        return account;
    }

    @Override
    public RunResult submitRun(PlayerAccount account, RunTally tally) {
        String token = account != null && !isBlank(account.token()) ? account.token() : tokenStore.getToken();
        String json = requireSuccess(send("/api/runs", "POST", submitBody(tally), token), 200, 201);
        JsonValue body = parseOrInvalid(json);
        return new RunResult(
                requireLong(body, "id"),
                requireString(body, "clientRunId"),
                requireString(body, "level"),
                requireInt(body, "perfect"),
                requireInt(body, "good"),
                requireInt(body, "miss"),
                requireLong(body, "durationMs"),
                requireBoolean(body, "completed"),
                requireInt(body, "score"),
                requireString(body, "submittedAt"));
    }

    @Override
    public List<LeaderboardRow> getLeaderboard(int limit) {
        if (limit < 1 || limit > 100) {
            throw new BackendException(BackendException.Kind.INVALID, "Limit must be between 1 and 100");
        }
        String json = requireSuccess(
                send("/api/leaderboard?limit=" + limit, "GET", null, tokenStore.getToken()), 200);
        JsonValue body = parseOrInvalid(json);
        if (!body.isArray()) {
            throw new BackendException(BackendException.Kind.INVALID, MALFORMED_RESPONSE);
        }
        List<LeaderboardRow> rows = new ArrayList<>();
        for (JsonValue child = body.child; child != null; child = child.next) {
            if (!child.isObject()) {
                throw new BackendException(BackendException.Kind.INVALID, MALFORMED_RESPONSE);
            }
            rows.add(new LeaderboardRow(
                    requireLong(child, "rank"),
                    requireString(child, "username"),
                    requireInt(child, "score"),
                    requireString(child, "submittedAt")));
        }
        return rows;
    }

    private HttpResponse<String> send(String path, String method, String body, String authToken) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(requestTimeout)
                .header("Content-Type", "application/json");
        if (!isBlank(authToken)) {
            request.header("Authorization", "Bearer " + authToken);
        }
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
        try {
            return client.send(request.method(method, publisher).build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new BackendException(BackendException.Kind.TRANSPORT, "Cannot reach the backend", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackendException(BackendException.Kind.TRANSPORT, "Request interrupted", e);
        }
    }

    private static String requireSuccess(HttpResponse<String> response, int... accepted) {
        int status = response.statusCode();
        for (int acceptedStatus : accepted) {
            if (status == acceptedStatus) {
                return response.body();
            }
        }
        throw new BackendException(kindFor(status), errorMessage(status, response.body()));
    }

    private static BackendException.Kind kindFor(int status) {
        if (status == 401) {
            return BackendException.Kind.UNAUTHORIZED;
        }
        if (status == 409) {
            return BackendException.Kind.CONFLICT;
        }
        if (status >= 300 && status < 400) {
            return BackendException.Kind.TRANSPORT;
        }
        if (status >= 400 && status < 500) {
            return BackendException.Kind.INVALID;
        }
        return BackendException.Kind.SERVER;
    }

    private static String errorMessage(int status, String body) {
        JsonValue error = tryParse(body);
        String message = null;
        String fieldErrors = null;
        if (error != null && error.isObject()) {
            JsonValue messageNode = error.get("message");
            if (messageNode != null && messageNode.isString()) {
                message = messageNode.asString();
            }
            JsonValue fields = error.get("fieldErrors");
            if (fields != null && fields.isObject()) {
                StringBuilder details = new StringBuilder();
                for (JsonValue child = fields.child; child != null; child = child.next) {
                    if (!child.isString()) {
                        continue;
                    }
                    if (details.length() > 0) {
                        details.append(", ");
                    }
                    details.append(child.name()).append(": ").append(child.asString());
                }
                if (details.length() > 0) {
                    fieldErrors = details.toString();
                }
            }
        }
        if (message == null || message.isBlank()) {
            message = genericMessage(status);
        }
        return fieldErrors == null ? message : message + " (" + fieldErrors + ")";
    }

    private static String genericMessage(int status) {
        return switch (kindFor(status)) {
            case UNAUTHORIZED -> "Sign in required";
            case CONFLICT -> "The request conflicts with an existing record";
            case INVALID -> "The request was rejected";
            case SERVER, TRANSPORT -> "The backend could not complete the request";
        };
    }

    private static JsonValue parseOrInvalid(String json) {
        JsonValue value = tryParse(json);
        if (value == null) {
            throw new BackendException(BackendException.Kind.INVALID, MALFORMED_RESPONSE);
        }
        return value;
    }

    private static JsonValue tryParse(String json) {
        try {
            return new JsonReader().parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonValue requireField(JsonValue object, String name) {
        JsonValue field = object.get(name);
        if (field == null || field.isNull()) {
            throw new BackendException(BackendException.Kind.INVALID, MALFORMED_RESPONSE);
        }
        return field;
    }

    private static String requireString(JsonValue object, String name) {
        JsonValue field = requireField(object, name);
        if (!field.isString() || field.asString().isBlank()) {
            throw new BackendException(BackendException.Kind.INVALID, MALFORMED_RESPONSE);
        }
        return field.asString();
    }

    private static int requireInt(JsonValue object, String name) {
        JsonValue field = requireField(object, name);
        if (!field.isNumber()) {
            throw new BackendException(BackendException.Kind.INVALID, MALFORMED_RESPONSE);
        }
        return field.asInt();
    }

    private static long requireLong(JsonValue object, String name) {
        JsonValue field = requireField(object, name);
        if (!field.isNumber()) {
            throw new BackendException(BackendException.Kind.INVALID, MALFORMED_RESPONSE);
        }
        return field.asLong();
    }

    private static boolean requireBoolean(JsonValue object, String name) {
        JsonValue field = requireField(object, name);
        if (!field.isBoolean()) {
            throw new BackendException(BackendException.Kind.INVALID, MALFORMED_RESPONSE);
        }
        return field.asBoolean();
    }

    private static String authBody(String username, String password) {
        return "{" + field("username", quote(username)) + "," + field("password", quote(password)) + "}";
    }

    private static String submitBody(RunTally tally) {
        StringBuilder json = new StringBuilder("{")
                .append(field("clientRunId", quote(tally.clientRunId())));
        if (!isBlank(tally.level())) {
            json.append(",").append(field("level", quote(tally.level())));
        }
        return json.append(",").append(field("perfect", Integer.toString(tally.perfect())))
                .append(",").append(field("good", Integer.toString(tally.good())))
                .append(",").append(field("miss", Integer.toString(tally.miss())))
                .append(",").append(field("durationMs", Long.toString(tally.durationMs())))
                .append(",").append(field("completed", Boolean.toString(tally.completed())))
                .append("}").toString();
    }

    private static String field(String name, String value) {
        return quote(name) + ":" + value;
    }

    private static String quote(String value) {
        if (value == null) {
            return "null";
        }
        StringBuilder out = new StringBuilder(value.length() + 2).append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append('"').toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
