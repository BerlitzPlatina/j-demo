package com.example.springaiopenai.dto;

import com.example.springaiopenai.entity.Order;

import java.math.BigDecimal;

/**
 * <p>
 * Bản rút gọn của đơn hàng, dùng khi liệt kê nhiều đơn cho model
 * </p>
 */
public record OrderSummary(
        String orderCode,
        String status,
        String location,
        BigDecimal amount,
        String createTime) {

    public static OrderSummary of(Order order) {
        return new OrderSummary(order.getOrderCode(), order.getStatus(), order.getLocation(), order.getAmount(),
                order.getCreateTime() == null ? null : order.getCreateTime().toString());
    }
}
