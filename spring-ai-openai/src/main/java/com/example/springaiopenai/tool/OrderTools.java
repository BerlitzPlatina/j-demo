package com.example.springaiopenai.tool;

import com.example.springaiopenai.dto.OrderStatusResponse;
import com.example.springaiopenai.dto.OrderSummary;
import com.example.springaiopenai.entity.Order;
import com.example.springaiopenai.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * Tập hợp tool cho LLM. Mỗi tool đọc/ghi database qua service → dao (JdbcTemplate),
 * không dùng dữ liệu mock.
 * </p>
 */
@Slf4j
@Component
public class OrderTools {
    private final IOrderService orderService;

    public OrderTools(IOrderService orderService) {
        this.orderService = orderService;
    }

    @Tool(name = "getOrderStatus", description = "Tra cứu trạng thái, vị trí và mã vận đơn của một đơn hàng theo mã đơn (ví dụ ORD-1001) trong database")
    public OrderStatusResponse getOrderStatus(
            @ToolParam(description = "Mã đơn hàng, ví dụ ORD-1001") String orderCode) {
        log.info("[tool] getOrderStatus orderCode={}", orderCode);
        try {
            Order order = orderService.getByOrderCode(orderCode);
            return order == null ? OrderStatusResponse.notFound(orderCode) : OrderStatusResponse.of(order);
        } catch (IllegalArgumentException e) {
            return OrderStatusResponse.error(orderCode, e.getMessage());
        }
    }

    @Tool(name = "findOrdersByCustomer", description = "Liệt kê các đơn hàng của một khách hàng theo tên (khớp gần đúng), sắp xếp mới nhất trước")
    public List<OrderSummary> findOrdersByCustomer(
            @ToolParam(description = "Tên khách hàng, có thể là một phần của tên") String customerName,
            @ToolParam(description = "Số đơn tối đa cần lấy, mặc định 5", required = false) Integer limit) {
        log.info("[tool] findOrdersByCustomer customerName={} limit={}", customerName, limit);
        List<Order> orders = orderService.findByCustomerName(customerName, limit == null ? 5 : limit);
        return orders.stream().map(OrderSummary::of).toList();
    }

    @Tool(name = "countOrdersByStatus", description = "Đếm số đơn hàng đang ở một trạng thái: NEW, PACKING, SHIPPING, DELIVERED hoặc CANCELLED")
    public Map<String, Object> countOrdersByStatus(
            @ToolParam(description = "Trạng thái đơn hàng: NEW, PACKING, SHIPPING, DELIVERED, CANCELLED") String status) {
        log.info("[tool] countOrdersByStatus status={}", status);
        return Map.of("status", status, "total", orderService.countByStatus(status));
    }

    @Tool(name = "cancelOrder", description = "Huỷ một đơn hàng theo mã đơn. Đơn đã giao thành công (DELIVERED) thì không huỷ được")
    public OrderStatusResponse cancelOrder(
            @ToolParam(description = "Mã đơn hàng cần huỷ, ví dụ ORD-1001") String orderCode) {
        log.info("[tool] cancelOrder orderCode={}", orderCode);
        try {
            return OrderStatusResponse.of(orderService.cancelOrder(orderCode));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return OrderStatusResponse.error(orderCode, e.getMessage());
        }
    }
}
