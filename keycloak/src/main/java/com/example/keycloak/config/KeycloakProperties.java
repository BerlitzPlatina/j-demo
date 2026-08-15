package com.example.keycloak.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * Keycloak settings for this API.
 * </p>
 *
 * @param audience     audience the incoming tokens must carry; a token minted for another client is
 *                     rejected
 * @param clientId     client used when exchanging credentials for a token on the login endpoint
 * @param clientSecret secret for {@code clientId}; empty for a public client
 * @author NamHoang
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(String audience, String clientId, String clientSecret) {

    public KeycloakProperties {
        if (clientId == null || clientId.isBlank()) {
            clientId = audience;
        }
        if (clientSecret == null) {
            clientSecret = "";
        }
    }

    public boolean hasClientSecret() {
        return !clientSecret.isBlank();
    }
}
