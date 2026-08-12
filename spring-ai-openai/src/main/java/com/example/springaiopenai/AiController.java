package com.example.springaiopenai;

import com.example.springaiopenai.tool.OrderTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
public class AiController {

    // Prompt ngắn: chỉ nêu vai trò + ràng buộc, việc chọn tool nào để cho description của
    // @Tool lo (mô tả tool đã được gửi kèm trong mỗi request, không cần lặp lại ở đây).
    private static final String ORDER_ASSISTANT_SYSTEM_PROMPT = """
            Bạn là trợ lý CSKH của shop online. Mọi dữ liệu đơn hàng chỉ được lấy qua tool, không suy đoán.
            Nếu tool trả về found=false hoặc lỗi, nói rõ cho khách.
            Trả lời tiếng Việt, ngắn gọn.
            """;

    private final ChatClient chatClient;

    public AiController(ChatModel chatModel, OrderTools orderTools) {
        // Đăng ký các tool đọc/ghi database làm tool mặc định của ChatClient
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(orderTools)
                .build();
    }

    // Endpoint gọi tự do
    @GetMapping("/api/ai/ask")
    public String ask(@RequestParam String message) {
        if (message == null || message.isBlank()) {
            return "message must not be blank";
        }
        return chatClient.prompt(message).call().content();
    }

    // Endpoint dùng Prompt Template với biến {topic} và {language}
    @GetMapping("/api/ai/explain")
    public String explain(
            @RequestParam String topic,
            @RequestParam(defaultValue = "tiếng Việt") String language) {

        PromptTemplate template = new PromptTemplate(
                "Hãy giải thích khái niệm '{topic}' một cách đơn giản, dễ hiểu bằng {language}. " +
                        "Cho ví dụ thực tế nếu có thể.");

        String prompt = template.render(Map.of("topic", topic, "language", language));
        return chatClient.prompt(prompt).call().content();
    }

    // Endpoint System Prompt + User Prompt
    @PostMapping("/api/ai/chat")
    public String chat(@RequestBody ChatRequest request) {
        return chatClient.prompt()
                .system(request.systemPrompt())
                .user(request.userPrompt())
                .call()
                .content();
    }

    record ChatRequest(String systemPrompt, String userPrompt) {
    }

    // Endpoint Streaming — trả về từng token ngay khi LLM sinh ra
    @PostMapping(value = "/api/ai/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatRequest request) {
        return chatClient.prompt()
                .system(request.systemPrompt())
                .user(request.userPrompt())
                .stream()
                .content();
    }

    // Demo Tool Calling: model tự chọn tool, tool truy vấn MySQL qua JdbcTemplate,
    // sau đó model trả lời lại bằng câu tự nhiên
    @GetMapping("/api/ai/tool-demo")
    public String toolDemo(@RequestParam String question) {
        if (question == null || question.isBlank()) {
            return "question must not be blank";
        }

        return chatClient.prompt()
                .system(ORDER_ASSISTANT_SYSTEM_PROMPT)
                .user(question)
                .call()
                .content();
    }
}
