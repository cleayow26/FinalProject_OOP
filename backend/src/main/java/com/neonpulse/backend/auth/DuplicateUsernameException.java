package com.neonpulse.backend.auth;

import com.neonpulse.backend.common.ApiException;
import org.springframework.http.HttpStatus;

public class DuplicateUsernameException extends ApiException {

    public DuplicateUsernameException(String username) {
        super(HttpStatus.CONFLICT, "Username already exists: " + username);
    }
}
