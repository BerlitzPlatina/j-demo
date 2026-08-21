package com.example.keycloak.config;

import com.example.common.security.KeycloakProperties;
import com.example.common.security.ResourceServerSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the resource server setup shared through the {@code common-security} jar is actually in
 * force in this application.
 * <p>
 * {@link ResourceServerSecurityConfig} sits outside this application's component-scan root and
 * reaches the context only through the {@code @Import} on
 * {@link com.example.keycloak.KeycloakApplication}. Drop that import and the application still
 * starts - it just serves every endpoint unsecured, which is the failure these tests exist to
 * catch. A plain {@code contextLoads()} test would not notice.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SharedSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApplicationContext context;

    /** The paths listed under keycloak.public-paths, and nothing else, are reachable without a token. */
    @Test
    void publicPathsFromConfigurationStayOpen() throws Exception {
        mockMvc.perform(get("/api/public")).andExpect(status().isOk());
    }

    @Test
    void everythingElseRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin")).andExpect(status().isUnauthorized());
    }

    /** A garbage token must be rejected by the decoder from the shared jar, not let through. */
    @Test
    void aMalformedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicPathsAreReadFromConfigurationRatherThanHardcoded() {
        assertThat(context.getBean(KeycloakProperties.class).publicPaths())
                .containsExactly("/api/auth/**", "/api/public");
    }
}
