package com.neonpulse.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.neonpulse.backend.auth.dto.AuthResponse;
import com.neonpulse.backend.auth.dto.LoginRequest;
import com.neonpulse.backend.auth.dto.RegisterRequest;
import com.neonpulse.backend.security.JwtService;
import com.neonpulse.backend.user.User;
import com.neonpulse.backend.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("dupe_user")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("dupe_user", "plain-password")))
                .isInstanceOf(DuplicateUsernameException.class);
        verify(userRepository, never()).insert(anyString(), anyString());
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userRepository.findByUsername("login_user"))
                .thenReturn(Optional.of(new User(1L, "login_user", "stored-hash", Instant.now())));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("login_user", "wrong-password")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).issueToken(anyString());
    }

    @Test
    void registerIssuesTokenForNewUser() {
        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("bcrypt-hash");
        when(userRepository.insert("new_user", "bcrypt-hash"))
                .thenReturn(new User(2L, "new_user", "bcrypt-hash", Instant.now()));
        when(jwtService.issueToken("new_user")).thenReturn("signed.jwt.token");

        AuthResponse response = authService.register(new RegisterRequest("new_user", "plain-password"));

        assertThat(response.token()).isEqualTo("signed.jwt.token");
        assertThat(response.username()).isEqualTo("new_user");
    }
}
