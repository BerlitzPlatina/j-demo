package com.example.springaiopenai.service;

import com.example.springaiopenai.entity.Order;

import java.util.List;

/**
 * <p>
 * Order Service
 * </p>
 */
public interface IOrderService {
    /**
     * Lấy đơn hàng theo mã đơn
     *
     * @param orderCode mã đơn hàng
     * @return đơn hàng, hoặc {@code null} nếu không tồn tại
     */
    Order getByOrderCode(String orderCode);

    /**
     * Tìm đơn hàng theo tên khách hàng
     *
     * @param customerName tên khách hàng
     * @param limit        số bản ghi tối đa
     * @return danh sách đơn hàng
     */
    List<Order> findByCustomerName(String customerName, int limit);

    /**
     * Đếm số đơn theo trạng thái
     *
     * @param status trạng thái đơn
     * @return số đơn
     */
    Long countByStatus(String status);

    /**
     * Huỷ đơn hàng theo mã đơn
     *
     * @param orderCode mã đơn hàng
     * @return đơn hàng sau khi huỷ
     * @throws IllegalStateException nếu đơn không tồn tại hoặc không thể huỷ
     */
    Order cancelOrder(String orderCode);
}
