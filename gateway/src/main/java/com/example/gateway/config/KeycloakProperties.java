package com.example.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * <p>
 * Keycloak settings for the edge.
 * </p>
 * <p>
 * Same property names as the services use, so one realm is described the same way everywhere -
 * but a separate class: this module runs on a different Spring Boot major than the services and
 * cannot link against their jar. See the note in the pom.
 * </p>
 *
 * @param audience    audience the incoming tokens must carry
 * @param publicPaths routes reachable without a token. Empty means every route needs a valid token.
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(String audience, List<String> publicPaths) {

    public KeycloakProperties {
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }
}
