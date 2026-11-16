package com.neonpulse.backend.auth;

import com.neonpulse.backend.auth.dto.AuthResponse;
import com.neonpulse.backend.auth.dto.LoginRequest;
import com.neonpulse.backend.auth.dto.RegisterRequest;
import com.neonpulse.backend.security.JwtService;
import com.neonpulse.backend.user.User;
import com.neonpulse.backend.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }
        String passwordHash = passwordEncoder.encode(request.password());
        User user = userRepository.insert(request.username(), passwordHash);
        return new AuthResponse(jwtService.issueToken(user.username()), user.username());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username()).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResponse(jwtService.issueToken(user.username()), user.username());
    }
}
