package com.example.springaiopenai;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderService {

    private final Map<Long, OrderStatusResponse> mockOrders = Map.of(
            123L, new OrderStatusResponse(123L, "Đã gửi cho đơn vị vận chuyển", "Hà Nội", "VN-123-456"),
            456L, new OrderStatusResponse(456L, "Đang đóng gói", "Hồ Chí Minh", "VN-456-789"),
            789L, new OrderStatusResponse(789L, "Đã giao thành công", "Đà Nẵng", "VN-789-999"));

    public OrderStatusResponse getOrderStatus(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId không được null");
        }

        return mockOrders.getOrDefault(orderId,
                new OrderStatusResponse(orderId, "Không tìm thấy đơn hàng", null, null));
    }
}
