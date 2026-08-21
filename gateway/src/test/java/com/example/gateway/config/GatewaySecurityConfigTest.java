package com.example.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the edge decisions that a typo in {@code keycloak.public-paths} would silently invert:
 * either every route becomes open, or the open ones stop working.
 * <p>
 * The real decoder reaches out to the Keycloak realm when it is built, so it is replaced here -
 * these tests are about which requests are let through, not about token validation itself. Nothing
 * is routed anywhere: security runs before routing, so a rejected request never needs a backend.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private KeycloakProperties properties;

    @MockitoBean
    private ReactiveJwtDecoder jwtDecoder;

    @Test
    void aRouteThatIsNotPublicIsRejectedWithoutAToken() {
        webClient.get().uri("/api/me").exchange().expectStatus().isUnauthorized();
    }

    /** An unknown path must not be a way around the check either. */
    @Test
    void anUnmatchedPathIsRejectedTooRatherThanRevealingWhatExists() {
        webClient.get().uri("/nothing-here").exchange().expectStatus().isUnauthorized();
    }

    /**
     * A public route must get past security. It is not routed anywhere in this test - no backend is
     * running - so the only thing asserted is that the rejection did not happen.
     */
    @Test
    void aPublicRouteIsNotRejectedByTheEdgeCheck() {
        webClient.get().uri("/api/public").exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(401));
    }

    @Test
    void openRoutesComeFromConfigurationRatherThanCode() {
        assertThat(properties.publicPaths()).containsExactly("/api/auth/**", "/api/public");
    }
}
