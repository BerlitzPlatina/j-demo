package com.example.keycloak.service;

import com.example.keycloak.config.KeycloakProperties;
import com.example.keycloak.dto.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/**
 * <p>
 * Talks to Keycloak's token endpoint on behalf of the caller.
 * </p>
 * <p>
 * This is what makes a login route possible at all: an OAuth2 resource server only ever validates
 * tokens, it cannot mint them. So the API forwards the credentials to Keycloak, and Keycloak decides.
 * The password never leaves this call and is not stored anywhere.
 * </p>
 *
 * @author NamHoang
 */
@Service
@Slf4j
public class KeycloakAuthService {

    private final RestClient restClient;
    private final KeycloakProperties properties;
    private final String tokenUri;
    private final String logoutUri;

    public KeycloakAuthService(KeycloakProperties properties,
                               @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        // Created directly rather than injecting RestClient.Builder: Spring Boot 4 moved that
        // auto-configuration into a separate starter this module does not need.
        this.restClient = RestClient.create();
        this.properties = properties;
        this.tokenUri = issuerUri + "/protocol/openid-connect/token";
        this.logoutUri = issuerUri + "/protocol/openid-connect/logout";
    }

    /**
     * Exchanges a username and password for tokens (OAuth2 password grant).
     *
     * @param username Keycloak username
     * @param password Keycloak password
     * @return the issued tokens
     */
    public TokenResponse login(String username, String password) {
        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "password");
        form.add("username", username);
        form.add("password", password);

        return post(form, "login for user " + username);
    }

    /**
     * Exchanges a refresh token for a fresh access token.
     *
     * @param refreshToken refresh token from a previous login
     * @return the issued tokens
     */
    public TokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> form = baseForm();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", refreshToken);

        return post(form, "token refresh");
    }

    /**
     * Ends the Keycloak session tied to the refresh token, which stops it from being used again.
     * <p>
     * Access tokens already handed out stay valid until they expire: they are self-contained and are
     * verified against the signing key, not against a session. Short token lifetimes are the answer
     * to that, not a logout call.
     *
     * @param refreshToken refresh token to invalidate
     */
    public void logout(String refreshToken) {
        MultiValueMap<String, String> form = baseForm();
        form.add("refresh_token", refreshToken);

        try {
            restClient.post().uri(logoutUri).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw translate(e, "logout");
        }
    }

    private MultiValueMap<String, String> baseForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        if (properties.hasClientSecret()) {
            form.add("client_secret", properties.clientSecret());
        }
        return form;
    }

    private TokenResponse post(MultiValueMap<String, String> form, String what) {
        try {
            return restClient.post().uri(tokenUri).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(TokenResponse.class);
        } catch (RestClientResponseException e) {
            throw translate(e, what);
        }
    }

    /**
     * Maps a Keycloak error onto a status for our own caller. Keycloak answers bad credentials with
     * 400 invalid_grant, which would be misleading to pass through as-is.
     */
    private ResponseStatusException translate(RestClientResponseException e, String what) {
        String body = e.getResponseBodyAsString();
        log.warn("Keycloak rejected {}: {} {}", what, e.getStatusCode(), body);

        if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 401) {
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials or token");
        }
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak is unavailable or misconfigured");
    }
}
