package com.example.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * Turns the roles Keycloak puts in a token into Spring Security authorities.
 * </p>
 * <p>
 * Keycloak does not use the {@code scope} claim for roles, so the default converter would find no
 * authorities at all and every {@code hasRole(...)} check would fail. Roles arrive in two places:
 * </p>
 * <pre>
 * "realm_access":    { "roles": ["admin", "user"] }                        // realm roles
 * "resource_access": { "spring-api": { "roles": ["reader"] } }             // client roles
 * </pre>
 * <p>
 * Both are read here and prefixed with {@code ROLE_}, which is what {@code hasRole("ADMIN")}
 * expects to find.
 * </p>
 *
 * @author NamHoang
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String REALM_ACCESS = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES = "roles";
    private static final String ROLE_PREFIX = "ROLE_";

    private final String clientId;

    /**
     * @param clientId the client whose roles under {@code resource_access} are read; realm roles are
     *                 always read
     */
    public KeycloakRealmRoleConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return Stream.concat(realmRoles(jwt).stream(), clientRoles(jwt).stream())
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role.toUpperCase()))
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<String> realmRoles(Jwt jwt) {
        return rolesIn(jwt.getClaimAsMap(REALM_ACCESS));
    }

    private List<String> clientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
        if (resourceAccess == null || !(resourceAccess.get(clientId) instanceof Map<?, ?> client)) {
            return List.of();
        }
        return rolesIn(client);
    }

    /**
     * Reads the {@code roles} list out of one of the access maps, tolerating a missing or
     * differently shaped claim rather than throwing — a token without roles is a valid token.
     */
    private List<String> rolesIn(Map<?, ?> access) {
        if (access == null || !(access.get(ROLES) instanceof Collection<?> roles)) {
            return List.of();
        }
        return roles.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }
}
