package com.example.springaiopenai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
class SpringAiOpenaiApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatModel chatModel;

    @BeforeEach
    void setUp() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("mock reply")))));
        when(chatModel.stream(any(Prompt.class)))
                .thenReturn(Flux.just(
                        new ChatResponse(List.of(new Generation(new AssistantMessage("token-1")))),
                        new ChatResponse(List.of(new Generation(new AssistantMessage("token-2"))))));
    }

    @Test
    void ask_shouldReturnModelResponse_whenMessageIsProvided() throws Exception {
        mockMvc.perform(get("/api/ai/ask").param("message", "hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("mock reply"));

        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void ask_shouldReturnValidationMessage_whenMessageIsBlank() throws Exception {
        mockMvc.perform(get("/api/ai/ask").param("message", "   "))
                .andExpect(status().isOk())
                .andExpect(content().string("message must not be blank"));

        verifyNoInteractions(chatModel);
    }

    @Test
    void explain_shouldRenderTemplateAndReturnModelResponse() throws Exception {
        mockMvc.perform(get("/api/ai/explain")
                .param("topic", "Spring Boot")
                .param("language", "tiếng Việt"))
                .andExpect(status().isOk())
                .andExpect(content().string("mock reply"));

        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void chat_shouldReturnModelResponse_usingSystemAndUserPrompt() throws Exception {
        String requestBody = "{\"systemPrompt\":\"You are a helpful assistant\",\"userPrompt\":\"What is Spring Boot?\"}";

        mockMvc.perform(post("/api/ai/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string("mock reply"));

        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void stream_shouldReturnTextEventStream() throws Exception {
        String requestBody = "{\"systemPrompt\":\"You are a helpful assistant\",\"userPrompt\":\"Say hi\"}";

        mockMvc.perform(post("/api/ai/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString(MediaType.TEXT_EVENT_STREAM_VALUE)))
                .andExpect(content().string(containsString("token-1")))
                .andExpect(content().string(containsString("token-2")));

        verify(chatModel).stream(any(Prompt.class));
    }
}
