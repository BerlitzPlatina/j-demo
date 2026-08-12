package com.example.springaiopenai;

public record OrderStatusResponse(
        Long orderId,
        String status,
        String location,
        String trackingCode) {
}
