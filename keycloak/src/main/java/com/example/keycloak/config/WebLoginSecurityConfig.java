package com.example.keycloak.config;

import com.example.common.security.KeycloakProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Map;

/**
 * <p>
 * Browser login for the Thymeleaf pages, using the OAuth2 Authorization Code flow.
 * </p>
 * <pre>
 * browser -> /home (no session)        -> 302 to Keycloak /auth
 * user types username/password on Keycloak's page
 * Keycloak -> /login/oauth2/code/keycloak?code=...
 * this app -> Keycloak /token (code + client credentials, or PKCE verifier for a public client)
 * Keycloak -> access token + id token + refresh token, kept in the HTTP session
 * </pre>
 * <p>
 * This chain only covers the UI paths and is ordered ahead of the stateless resource-server chain
 * from common-security, which keeps handling everything under {@code /api}.
 * </p>
 *
 * @author NamHoang
 */
@Configuration
public class WebLoginSecurityConfig {

    static final String REGISTRATION_ID = "keycloak";

    @Bean
    @Order(1)
    public SecurityFilterChain webLoginFilterChain(HttpSecurity http,
                                                   ClientRegistrationRepository clientRegistrations) throws Exception {
        OidcClientInitiatedLogoutSuccessHandler logoutSuccessHandler =
                new OidcClientInitiatedLogoutSuccessHandler(clientRegistrations);
        // Must be listed under "Valid post logout redirect URIs" on the Keycloak client.
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");

        http
                .securityMatcher("/", "/home", "/logout", "/oauth2/**", "/login/**", "/css/**", "/error")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        // Skip Spring's provider picker page and go straight to Keycloak.
                        .loginPage("/oauth2/authorization/" + REGISTRATION_ID)
                        .defaultSuccessUrl("/home", true))
                // Ends the Keycloak SSO session as well, not only the local HTTP session.
                .logout(logout -> logout.logoutSuccessHandler(logoutSuccessHandler));

        return http.build();
    }

    /**
     * Built by hand rather than from {@code spring.security.oauth2.client.provider.*.issuer-uri}:
     * that property makes Spring fetch the discovery document at startup, so the application would
     * not even start (nor would the tests) while Keycloak is down. The endpoint paths below are
     * fixed by Keycloak for every realm.
     * <p>
     * With {@code keycloak.client-secret} set the code is exchanged using the secret (confidential
     * client); left empty, the client is treated as public and Spring protects the exchange with
     * PKCE instead.
     */
    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            KeycloakProperties properties,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        String oidc = issuerUri + "/protocol/openid-connect";

        ClientRegistration keycloak = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId(properties.clientId())
                .clientSecret(properties.clientSecret())
                .clientAuthenticationMethod(properties.hasClientSecret()
                        ? ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                        : ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // Must be listed under "Valid redirect URIs" on the Keycloak client.
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .issuerUri(issuerUri)
                .authorizationUri(oidc + "/auth")
                .tokenUri(oidc + "/token")
                .jwkSetUri(oidc + "/certs")
                .userInfoUri(oidc + "/userinfo")
                .userNameAttributeName("preferred_username")
                .providerConfigurationMetadata(Map.of("end_session_endpoint", oidc + "/logout"))
                .clientName("Keycloak")
                .build();

        return new InMemoryClientRegistrationRepository(keycloak);
    }
}
