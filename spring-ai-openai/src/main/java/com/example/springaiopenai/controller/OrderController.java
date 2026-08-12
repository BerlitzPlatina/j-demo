package com.example.springaiopenai.controller;

import com.example.springaiopenai.dto.OrderStatusResponse;
import com.example.springaiopenai.dto.OrderSummary;
import com.example.springaiopenai.entity.Order;
import com.example.springaiopenai.service.IOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * REST endpoint gọi trực tiếp tầng ORM, dùng để kiểm tra database mà không cần LLM
 * </p>
 */
@RestController
public class OrderController {
    private final IOrderService orderService;

    public OrderController(IOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/api/orders/{orderCode}")
    public OrderStatusResponse getByOrderCode(@PathVariable String orderCode) {
        Order order = orderService.getByOrderCode(orderCode);
        return order == null ? OrderStatusResponse.notFound(orderCode) : OrderStatusResponse.of(order);
    }

    @GetMapping("/api/orders")
    public List<OrderSummary> findByCustomer(
            @RequestParam String customerName,
            @RequestParam(defaultValue = "5") int limit) {
        return orderService.findByCustomerName(customerName, limit).stream().map(OrderSummary::of).toList();
    }

    @GetMapping("/api/orders/count")
    public Map<String, Object> countByStatus(@RequestParam String status) {
        return Map.of("status", status, "total", orderService.countByStatus(status));
    }

    @PostMapping("/api/orders/{orderCode}/cancel")
    public OrderStatusResponse cancel(@PathVariable String orderCode) {
        return OrderStatusResponse.of(orderService.cancelOrder(orderCode));
    }
}
