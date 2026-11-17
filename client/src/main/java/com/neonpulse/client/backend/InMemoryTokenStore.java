package com.neonpulse.client.backend;

public class InMemoryTokenStore implements TokenStore {
    private volatile String token;

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public void saveToken(String token) {
        this.token = token;
    }

    @Override
    public void clear() {
        token = null;
    }
}
