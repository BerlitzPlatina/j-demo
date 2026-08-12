package com.example.springaiopenai.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * <p>
 * Vá lỗi tool calling khi dùng Gemini qua endpoint OpenAI-compatible.
 * </p>
 * <p>
 * Gemini (các model thinking: gemini-flash-latest, gemini-3.x...) trả về kèm
 * {@code tool_calls[].extra_content.google.thought_signature} và bắt buộc client
 * phải gửi lại chữ ký đó ở lượt request tiếp theo. Spring AI 1.x (kể cả 1.1.x)
 * không có field này trong record ChatCompletionMessage.ToolCall nên chữ ký bị
 * mất → Gemini trả HTTP 400 "Function call is missing a thought_signature".
 * </p>
 * <p>
 * Interceptor dưới đây bắt ở tầng HTTP: đọc chữ ký trong response (theo tool call id)
 * và chèn lại vào request kế tiếp. Chỉ áp dụng cho RestClient (đường gọi blocking
 * mà tool calling dùng), không ảnh hưởng streaming (WebClient).
 * </p>
 */
@Slf4j
@Configuration
public class GeminiThoughtSignatureConfig {

    /** Giới hạn số chữ ký lưu tạm, tránh phình bộ nhớ. */
    private static final int MAX_CACHED_SIGNATURES = 500;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** tool call id -> thought_signature, LRU đơn giản. */
    private final Map<String, String> signatures = Collections.synchronizedMap(
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
                    return size() > MAX_CACHED_SIGNATURES;
                }
            });

    @Bean
    public RestClientCustomizer geminiThoughtSignatureCustomizer() {
        return builder -> builder.requestInterceptor((request, body, execution) -> {
            byte[] outgoing = injectSignatures(body);
            if (outgoing != body) {
                request.getHeaders().setContentLength(outgoing.length);
            }

            ClientHttpResponse response = execution.execute(request, outgoing);
            if (!isJson(response.getHeaders())) {
                return response;
            }

            byte[] responseBody = response.getBody().readAllBytes();
            captureSignatures(responseBody);
            return new BufferedResponse(response, responseBody);
        });
    }

    /** Chèn lại thought_signature vào các assistant tool_calls của request. */
    private byte[] injectSignatures(byte[] body) {
        if (body == null || body.length == 0 || signatures.isEmpty()) {
            return body;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode messages = root.path("messages");
            if (!messages.isArray()) {
                return body;
            }

            boolean changed = false;
            for (JsonNode message : messages) {
                JsonNode toolCalls = message.path("tool_calls");
                if (!toolCalls.isArray()) {
                    continue;
                }
                for (JsonNode toolCall : toolCalls) {
                    if (!(toolCall instanceof ObjectNode node) || node.has("extra_content")) {
                        continue;
                    }
                    String signature = signatures.get(node.path("id").asText());
                    if (signature == null) {
                        continue;
                    }
                    node.putObject("extra_content")
                            .putObject("google")
                            .put("thought_signature", signature);
                    changed = true;
                }
            }
            if (!changed) {
                return body;
            }
            log.debug("[gemini] đã chèn lại thought_signature vào request tool calling");
            return objectMapper.writeValueAsBytes(root);
        } catch (IOException e) {
            log.warn("[gemini] không parse được request body, bỏ qua việc chèn thought_signature: {}", e.getMessage());
            return body;
        }
    }

    /** Lưu thought_signature từ response để dùng cho lượt gọi sau. */
    private void captureSignatures(byte[] body) {
        if (body == null || body.length == 0) {
            return;
        }
        try {
            JsonNode choices = objectMapper.readTree(body).path("choices");
            if (!choices.isArray()) {
                return;
            }
            for (JsonNode choice : choices) {
                JsonNode toolCalls = choice.path("message").path("tool_calls");
                if (!(toolCalls instanceof ArrayNode)) {
                    continue;
                }
                for (JsonNode toolCall : toolCalls) {
                    String id = toolCall.path("id").asText(null);
                    String signature = toolCall.path("extra_content").path("google")
                            .path("thought_signature").asText(null);
                    if (id != null && signature != null && !signature.isBlank()) {
                        signatures.put(id, signature);
                        log.debug("[gemini] lưu thought_signature cho tool call id={}", id);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("[gemini] không parse được response body: {}", e.getMessage());
        }
    }

    private boolean isJson(HttpHeaders headers) {
        return headers.getContentType() != null
                && headers.getContentType().includes(org.springframework.http.MediaType.APPLICATION_JSON);
    }

    /** Bọc lại response vì body đã bị đọc hết để lấy chữ ký. */
    private record BufferedResponse(ClientHttpResponse delegate, byte[] body) implements ClientHttpResponse {

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }
    }
}
