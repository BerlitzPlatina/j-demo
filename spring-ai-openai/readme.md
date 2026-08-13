Luồng của /api/ai/tool-demo
Ví dụ bạn gọi: GET /api/ai/tool-demo?question=Có bao nhiêu đơn đang giao?


Browser
  │
  ▼ 1
AiController.toolDemo()                        AiController.java:85-96
  │  chatClient.prompt().system(PROMPT).user(question).call()
  ▼ 2
ChatClient  ──┐ (tool đã đăng ký sẵn ở constructor: .defaultTools(orderTools))
              │  Spring AI quét @Tool trong OrderTools → sinh JSON schema
  ▼ 3
OpenAiChatModel → RestClient  ─── HTTP POST #1 ──►  Gemini
                     ▲                              (messages + tools[])
                     │                                    │
      GeminiThoughtSignatureConfig  ◄── response #1 ───────┘
        (lưu thought_signature)      finish_reason=tool_calls
                     │                countOrdersByStatus{status:"SHIPPING"}
  ▼ 4
ToolCallingManager → OrderTools.countOrdersByStatus()   OrderTools.java:51
  │
  ▼ 5
OrderServiceImpl → OrderDao → JdbcTemplate → MySQL   →  2
  │
  ▼ 6
RestClient  ─── HTTP POST #2 ──►  Gemini
   ▲  (messages cũ + assistant tool_calls + role:tool content:"2")
   │
GeminiThoughtSignatureConfig chèn lại extra_content.thought_signature
                     │
                     ▼ 7
        "Hiện có 2 đơn hàng đang ở trạng thái đang giao (SHIPPING)."
                     │
                     ▼ 8
              String trả về cho browser
Chi tiết từng bước
1–2. Controller dựng prompt. ChatClient được build một lần trong constructor với .defaultTools(orderTools) — Spring AI đọc annotation @Tool / @ToolParam trong OrderTools.java, sinh ra 4 định nghĩa function kèm JSON schema tham số. Danh sách này được gửi kèm mọi request.

3. Request #1 — model quyết định gọi tool. Body gồm system prompt, câu hỏi của bạn, và tools[]. Gemini không tự trả lời mà trả về finish_reason: tool_calls + tên tool và tham số nó suy ra (status: "SHIPPING"). Ở đây model chỉ đề nghị gọi — nó không chạm được vào DB.

Interceptor trong GeminiThoughtSignatureConfig.java đọc response, cất extra_content.google.thought_signature vào cache theo tool call id.

4. Spring AI thực thi tool. ToolCallingManager map tên countOrdersByStatus → method Java, parse JSON arguments thành tham số, gọi method. Vòng lặp này Spring AI tự làm bên trong .call() — code controller của bạn không thấy gì.

5. Xuống database. OrderTools → IOrderService/OrderServiceImpl → OrderDao (JdbcTemplate) → MySQL. Kết quả Map{status:SHIPPING, total:2} được serialize thành JSON.

6. Request #2 — model diễn giải kết quả. Toàn bộ hội thoại được gửi lại: user question + assistant tool_calls + message role: "tool" chứa kết quả. Đây là chỗ trước đây lỗi 400 — Gemini bắt buộc phải thấy lại thought_signature trong tool_calls, mà record của Spring AI 1.x không có field đó. Interceptor chèn lại trước khi request bay đi.

7–8. Trả lời. Gemini lần này finish_reason: stop, sinh câu tiếng Việt từ số 2 lấy được. .content() bóc text ra, controller trả về String.

Vài điểm đáng chú ý
1 câu hỏi = 2 request LLM. Nếu model gọi 2 tool thì vẫn 2 request (gọi song song trong cùng một lượt), nhưng nếu nó gọi tool nối tiếp (kết quả tool A quyết định tham số tool B) thì thành 3+. Đây là lý do quota free tier hết nhanh.
Model không thấy database, chỉ thấy mô tả tool. Nên description trong @Tool chính là thứ quyết định model chọn đúng tool hay không — quan trọng hơn system prompt.
Đường blocking dùng RestClient, streaming dùng WebClient. Bản vá signature chỉ gắn vào RestClient, nên /api/ai/stream không bị ảnh hưởng (và cũng không có tool calling).