package com.example.keycloak.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Endpoints showing the three access levels: open, any authenticated user, and admin only.
 * </p>
 *
 * @author NamHoang
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

    /**
     * Open endpoint, listed under permitAll in the security configuration.
     */
    @GetMapping("/public")
    public Map<String, Object> publicEndpoint() {
        return Map.of("message", "No token needed for this one");
    }

    /**
     * Any valid token gets in. Shows what the API can read out of the token.
     */
    @GetMapping("/me")
    public Map<String, Object> me(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        return Map.of(
                "username", authentication.getName(),
                "email", String.valueOf(jwt.getClaimAsString("email")),
                "subject", String.valueOf(jwt.getSubject()),
                "issuer", String.valueOf(jwt.getIssuer()),
                "audience", jwt.getAudience(),
                "expiresAt", String.valueOf(jwt.getExpiresAt()),
                "authorities", authorities(authentication));
    }

    /**
     * Requires the realm role {@code user}, which the converter turns into {@code ROLE_USER}.
     */
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> userEndpoint(Authentication authentication) {
        log.debug("User endpoint reached by {}", authentication.getName());
        return Map.of("message", "Visible to anyone holding the user role", "username", authentication.getName());
    }

    /**
     * Requires the realm role {@code admin}. A token holding only {@code user} gets a 403 here.
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminEndpoint(Authentication authentication) {
        return Map.of("message", "Admins only", "username", authentication.getName(), "serverTime", Instant.now().toString());
    }

    private List<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
    }
}
