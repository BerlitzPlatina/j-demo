package com.example.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * <p>
 * Keycloak settings for a resource server.
 * </p>
 *
 * @param audience     audience the incoming tokens must carry; a token minted for another client is
 *                     rejected
 * @param clientId     client whose roles are read out of {@code resource_access}, and the client
 *                     used when exchanging credentials for a token; defaults to {@code audience}
 * @param clientSecret secret for {@code clientId}; empty for a public client
 * @param publicPaths  endpoints reachable without a token. Left to configuration on purpose: which
 *                     routes are open differs per service, and it is the one part of the shared
 *                     setup that must not be decided in this jar. Empty means every request needs
 *                     a valid token.
 * @author NamHoang
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(String audience, String clientId, String clientSecret,
                                 List<String> publicPaths) {

    public KeycloakProperties {
        if (clientId == null || clientId.isBlank()) {
            clientId = audience;
        }
        if (clientSecret == null) {
            clientSecret = "";
        }
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }

    public boolean hasClientSecret() {
        return !clientSecret.isBlank();
    }
}
