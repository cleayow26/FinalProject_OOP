package com.neonpulse.client.backend;

public interface TokenStore {
    String getToken();

    void saveToken(String token);

    void clear();
}
