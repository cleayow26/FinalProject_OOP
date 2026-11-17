package com.neonpulse.client.backend;

public class BackendException extends RuntimeException {

    public enum Kind {
        UNAUTHORIZED,
        CONFLICT,
        INVALID,
        SERVER,
        TRANSPORT
    }

    private final Kind kind;

    public BackendException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public BackendException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }
}
