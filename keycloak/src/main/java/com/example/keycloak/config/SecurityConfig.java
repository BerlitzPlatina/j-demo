package com.example.keycloak.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtAudienceValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * <p>
 * Secures the API as an OAuth2 resource server backed by Keycloak.
 * </p>
 * <p>
 * The API never sees a password and issues nothing: the client gets a token from Keycloak, sends it
 * as {@code Authorization: Bearer ...}, and this configuration checks the signature, the issuer, the
 * expiry and the audience before mapping the token's roles onto Spring authorities.
 * </p>
 *
 * @author NamHoang
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final String issuerUri;
    private final KeycloakProperties properties;

    public SecurityConfig(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                          KeycloakProperties properties) {
        this.issuerUri = issuerUri;
        this.properties = properties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // No browser session and no login form: every request carries its own token.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // An error is rendered by forwarding to /error, a second pass through this
                        // chain. Without this the 400 from a malformed login body would come back as
                        // a 401, because /error itself is not permitted.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        // Login has to be open, otherwise there is no way to obtain a token.
                        .requestMatchers("/api/auth/**", "/api/public").permitAll()
                        // Role checks for the remaining endpoints live on the controller methods,
                        // via @PreAuthorize; here we only require a valid token.
                        .anyRequest().authenticated())

                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Decoder for Keycloak tokens. {@code withIssuerLocation} fetches the realm's OIDC discovery
     * document once at startup and takes the JWKS url from it, so rotating Keycloak's signing keys
     * needs no change here.
     * <p>
     * The audience check is the part Spring does not apply by default: without it any token from
     * this realm would be accepted, including one issued to a completely different client.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator(issuerUri),
                new JwtAudienceValidator(properties.audience())));
        return decoder;
    }

    /**
     * Wires in the Keycloak role mapping and makes {@code getName()} return the username rather
     * than the opaque {@code sub} uuid.
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter(properties.clientId()));
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }
}
