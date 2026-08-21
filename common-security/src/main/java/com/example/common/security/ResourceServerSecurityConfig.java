package com.example.common.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * Secures an API as an OAuth2 resource server backed by Keycloak.
 * </p>
 * <p>
 * The API never sees a password and issues nothing: the client gets a token from Keycloak, sends it
 * as {@code Authorization: Bearer ...}, and this configuration checks the signature, the issuer, the
 * expiry and the audience before mapping the token's roles onto Spring authorities. Every service
 * validates locally against the realm's published keys, so no request is ever forwarded to an
 * authentication service.
 * </p>
 * <p>
 * This class lives in a jar, outside the component-scan root of the applications that use it, so an
 * application opts in explicitly:
 * <pre>
 * &#64;SpringBootApplication
 * &#64;Import(ResourceServerSecurityConfig.class)
 * public class MyApplication { ... }
 * </pre>
 * Which endpoints stay open is not decided here - set {@code keycloak.public-paths} per service.
 * A service that needs a different chain altogether simply does not import this class and declares
 * its own {@link SecurityFilterChain}; note that {@code @ConditionalOnMissingBean} would <em>not</em>
 * be a safe way to allow overriding, because ordering guarantees for that annotation hold only
 * inside auto-configuration classes, not in an imported {@code @Configuration}.
 * </p>
 *
 * @author NamHoang
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(KeycloakProperties.class)
public class ResourceServerSecurityConfig {

    private final String issuerUri;
    private final KeycloakProperties properties;

    public ResourceServerSecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
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

                .authorizeHttpRequests(auth -> {
                    // An error is rendered by forwarding to /error, a second pass through this
                    // chain. Without this the 400 from a malformed request body would come back as
                    // a 401, because /error itself is not permitted.
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    // requestMatchers() rejects an empty argument list, so only apply the exception
                    // when the service actually declared open endpoints.
                    if (!properties.publicPaths().isEmpty()) {
                        auth.requestMatchers(properties.publicPaths().toArray(String[]::new)).permitAll();
                    }
                    // Role checks for the remaining endpoints live on the controller methods,
                    // via @PreAuthorize; here we only require a valid token.
                    auth.anyRequest().authenticated();
                })

                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * Decoder for Keycloak tokens. {@code withIssuerLocation} fetches the realm's OIDC discovery
     * document once at startup and takes the JWKS url from it, so rotating Keycloak's signing keys
     * needs no change here.
     * <p>
     * The audience check is the part Spring does not apply by default: without it any token from
     * this realm would be accepted, including one issued to a completely different client. In a
     * multi-service setup that is exactly the check that stops a token minted for one service from
     * being replayed against another.
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
