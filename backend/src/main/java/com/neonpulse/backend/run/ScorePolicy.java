package com.neonpulse.backend.run;

public final class ScorePolicy {

    private ScorePolicy() {}

    public static int compute(int perfect, int good, int miss, long durationMs, boolean completed) {
        return perfect * 100 + good * 50 + (completed ? 500 : 0);
    }
}
