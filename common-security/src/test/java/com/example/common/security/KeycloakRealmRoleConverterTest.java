package com.example.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The role mapping is the part most likely to break silently: get it wrong and every
 * {@code @PreAuthorize} check fails while the token itself still validates. Tested here rather
 * than in a consuming service so a regression surfaces in the jar that owns it.
 */
class KeycloakRealmRoleConverterTest {

    private static final String CLIENT_ID = "spring-api";

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter(CLIENT_ID);

    private static Jwt jwtWith(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256");
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    void readsRealmRolesAndClientRolesAndPrefixesThem() {
        Jwt jwt = jwtWith(Map.of(
                "realm_access", Map.of("roles", List.of("admin", "user")),
                "resource_access", Map.of(CLIENT_ID, Map.of("roles", List.of("reader")))));

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER", "ROLE_READER");
    }

    /** Roles of another client must not leak into this service's authorities. */
    @Test
    void ignoresRolesBelongingToAnotherClient() {
        Jwt jwt = jwtWith(Map.of(
                "resource_access", Map.of("some-other-api", Map.of("roles", List.of("admin")))));

        assertThat(converter.convert(jwt)).isEmpty();
    }

    /** A token carrying no roles is still a valid token; the converter must not throw. */
    @Test
    void toleratesMissingAndMalformedClaims() {
        assertThat(converter.convert(jwtWith(Map.of("sub", "uuid")))).isEmpty();
        assertThat(converter.convert(jwtWith(Map.of("realm_access", Map.of("roles", "not-a-list"))))).isEmpty();
    }
}
