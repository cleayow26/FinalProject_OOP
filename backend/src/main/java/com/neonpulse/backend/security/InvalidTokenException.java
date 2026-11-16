package com.neonpulse.backend.security;

import com.neonpulse.backend.common.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ApiException {

    public InvalidTokenException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
    }
}
