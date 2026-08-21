package com.example.orm.jpa.exception;

import com.example.common.web.exception.GlobalExceptionHandler;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the exception handling shared through the {@code common-web} jar is actually wired into
 * this application's MVC layer.
 * <p>
 * The base class {@link GlobalExceptionHandler} sits outside this application's component-scan
 * root, so nothing would register it on its own; what makes it work is {@link
 * OrmJpaExceptionHandler}, the local {@code @RestControllerAdvice} subclass. These tests fail if
 * that indirection breaks - which a plain {@code contextLoads()} test would not catch.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SharedExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ApplicationContext context;

    @Test
    void adviceFromTheSharedJarIsRegisteredExactlyOnce() {
        assertThat(context.getBeansOfType(GlobalExceptionHandler.class)).hasSize(1);
    }

    /** ResourceNotFoundException travels from the shared jar through the inherited handler. */
    @Test
    void unknownIdBecomesTheSharedNotFoundEnvelope() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /** The inherited MethodArgumentNotValidException handler names the offending fields. */
    @Test
    void invalidBodyBecomesTheSharedBadRequestEnvelope() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","password":"x","email":"not-an-email","phoneNumber":"abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("email")));
    }

    /** The one hook this module overrides must win over the generic default. */
    @Test
    void duplicateNameUsesThisModulesConflictMessage() throws Exception {
        String body = """
                {"name":"dupuser","password":"secret","email":"dup@example.com","phoneNumber":"123456789"}
                """;
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("name, email and phoneNumber must be unique"));
    }
}
