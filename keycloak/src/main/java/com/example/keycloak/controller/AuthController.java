package com.example.keycloak.controller;

import com.example.keycloak.dto.LoginRequest;
import com.example.keycloak.dto.TokenResponse;
import com.example.keycloak.service.KeycloakAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * <p>
 * Login, refresh and logout. These are the only endpoints reachable without a token.
 * </p>
 *
 * @author NamHoang
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final KeycloakAuthService keycloakAuthService;

    /**
     * Exchanges credentials for a JWT issued by Keycloak.
     */
    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        requireText(request.username(), "username");
        requireText(request.password(), "password");

        log.debug("Login attempt for {}", request.username());
        return keycloakAuthService.login(request.username(), request.password());
    }

    /**
     * Issues a new access token from a refresh token, so the user does not have to log in again.
     */
    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        requireText(refreshToken, "refreshToken");

        return keycloakAuthService.refresh(refreshToken);
    }

    /**
     * Ends the Keycloak session behind the refresh token.
     */
    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        requireText(refreshToken, "refreshToken");

        keycloakAuthService.logout(refreshToken);
        return Map.of("loggedOut", true);
    }

    private void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must not be blank");
        }
    }
}
