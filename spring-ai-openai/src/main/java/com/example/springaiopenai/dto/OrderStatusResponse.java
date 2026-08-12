package com.example.springaiopenai.dto;

import com.example.springaiopenai.entity.Order;

import java.math.BigDecimal;

/**
 * <p>
 * Kết quả trả cho model khi tra cứu một đơn hàng
 * </p>
 */
public record OrderStatusResponse(
        boolean found,
        String orderCode,
        String customerName,
        String status,
        String location,
        String trackingCode,
        BigDecimal amount,
        String message) {

    public static OrderStatusResponse of(Order order) {
        return new OrderStatusResponse(true, order.getOrderCode(), order.getCustomerName(), order.getStatus(),
                order.getLocation(), order.getTrackingCode(), order.getAmount(), null);
    }

    public static OrderStatusResponse notFound(String orderCode) {
        return new OrderStatusResponse(false, orderCode, null, null, null, null, null,
                "Không tìm thấy đơn hàng " + orderCode + " trong database");
    }

    public static OrderStatusResponse error(String orderCode, String message) {
        return new OrderStatusResponse(false, orderCode, null, null, null, null, null, message);
    }
}
