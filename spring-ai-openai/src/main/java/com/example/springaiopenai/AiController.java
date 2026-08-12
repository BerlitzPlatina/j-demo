package com.example.springaiopenai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.tool.annotation.Tool;
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

    private final ChatClient chatClient;
    private final OrderService orderService;

    public AiController(ChatModel chatModel, OrderService orderService) {
        this.orderService = orderService;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultTools(this)
                .build();
    }

    @Tool(description = "Lấy trạng thái mock của đơn hàng theo orderId")
    public String getOrderStatus(Long orderId) {
        OrderStatusResponse response = orderService.getOrderStatus(orderId);

        if (response.location() == null) {
            return "Đơn hàng " + response.orderId() + " không tìm thấy trong dữ liệu mock.";
        }

        return "Đơn hàng " + response.orderId() + " đang ở trạng thái: "
                + response.status() + ". Vị trí hiện tại: " + response.location() + ".";
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

    // Demo Tool Calling: model sẽ gọi getOrderStatus(orderId), sau đó trả lời lại
    // cho user bằng câu tự nhiên
    @GetMapping("/api/ai/tool-demo")
    public String toolDemo(@RequestParam String question) {
        if (question == null || question.isBlank()) {
            return "question must not be blank";
        }

        return chatClient.prompt()
                .system("Bạn là trợ lý hỗ trợ khách hàng. Nếu câu hỏi liên quan đến đơn hàng, hãy tìm orderId trong câu hỏi, gọi tool getOrderStatus, rồi trả lời lại bằng tiếng Việt tự nhiên, ngắn gọn và rõ ràng.")
                .user(question)
                .call()
                .content();
    }
}
