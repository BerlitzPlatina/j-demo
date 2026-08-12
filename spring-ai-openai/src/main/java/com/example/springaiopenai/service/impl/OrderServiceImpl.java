package com.example.springaiopenai.service.impl;

import com.example.springaiopenai.dao.OrderDao;
import com.example.springaiopenai.entity.Order;
import com.example.springaiopenai.service.IOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * Order Service Implement, mọi truy cập DB đi qua {@link OrderDao}
 * </p>
 */
@Slf4j
@Service
public class OrderServiceImpl implements IOrderService {
    /**
     * Trạng thái đã giao thành công thì không cho huỷ
     */
    private static final String STATUS_DELIVERED = "DELIVERED";

    /**
     * Trạng thái đã huỷ
     */
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final OrderDao orderDao;

    public OrderServiceImpl(OrderDao orderDao) {
        this.orderDao = orderDao;
    }

    @Override
    public Order getByOrderCode(String orderCode) {
        if (orderCode == null || orderCode.isBlank()) {
            throw new IllegalArgumentException("orderCode không được rỗng");
        }
        return orderDao.selectByOrderCode(orderCode.trim());
    }

    @Override
    public List<Order> findByCustomerName(String customerName, int limit) {
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("customerName không được rỗng");
        }
        int size = limit <= 0 ? 5 : Math.min(limit, 50);
        return orderDao.selectByCustomerName(customerName.trim(), size);
    }

    @Override
    public Long countByStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status không được rỗng");
        }
        return orderDao.countByStatus(status.trim().toUpperCase());
    }

    @Override
    @Transactional
    public Order cancelOrder(String orderCode) {
        Order exist = getByOrderCode(orderCode);
        if (exist == null) {
            throw new IllegalStateException("Không tìm thấy đơn hàng " + orderCode);
        }
        if (STATUS_DELIVERED.equalsIgnoreCase(exist.getStatus())) {
            throw new IllegalStateException("Đơn hàng " + orderCode + " đã giao thành công, không thể huỷ");
        }
        if (STATUS_CANCELLED.equalsIgnoreCase(exist.getStatus())) {
            return exist;
        }

        // Chỉ set field cần đổi, BaseDao bỏ qua field null nên UPDATE chỉ chứa 2 cột
        Order update = new Order();
        update.setStatus(STATUS_CANCELLED);
        update.setLastUpdateTime(new Date());
        orderDao.update(update, exist.getId());
        log.info("Đã huỷ đơn hàng {}", orderCode);

        return orderDao.selectById(exist.getId());
    }
}
