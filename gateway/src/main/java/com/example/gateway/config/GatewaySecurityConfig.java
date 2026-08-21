package com.example.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.List;

/**
 * <p>
 * Rejects requests without a usable token before they ever reach a service.
 * </p>
 * <p>
 * This check is a filter, not the security boundary. Each service validates the same token again
 * for itself, because anything that reaches the internal network can call a service directly on its
 * own port and bypass this gateway entirely. The edge check exists to fail fast and to keep
 * obviously bad traffic off the services, not to let them trust the network.
 * </p>
 * <p>
 * Reactive counterpart of the servlet setup in the common-security jar; see the pom for why the two
 * are not shared.
 * </p>
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    private final String issuerUri;
    private final KeycloakProperties properties;

    public GatewaySecurityConfig(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            KeycloakProperties properties) {
        this.issuerUri = issuerUri;
        this.properties = properties;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                // Nothing here holds a session and no route serves a form: every request carries
                // its own token.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                .authorizeExchange(exchange -> {
                    // pathMatchers() rejects an empty argument list, so only apply the exception
                    // when open routes were actually configured.
                    if (!properties.publicPaths().isEmpty()) {
                        exchange.pathMatchers(properties.publicPaths().toArray(String[]::new)).permitAll();
                    }
                    exchange.anyExchange().authenticated();
                })

                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtDecoder(jwtDecoder())));

        return http.build();
    }

    /**
     * Same three checks the services apply: signature against the realm's published keys, issuer,
     * and audience. The audience check is what stops a token minted for a different client from
     * being replayed here; Spring does not apply it by default.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(
                new JwtIssuerValidator(issuerUri),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        audiences -> audiences != null && audiences.contains(properties.audience()))));
        return decoder;
    }
}
