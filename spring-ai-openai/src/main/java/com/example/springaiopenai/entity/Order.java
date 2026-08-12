package com.example.springaiopenai.entity;

import com.example.springaiopenai.annotation.Column;
import com.example.springaiopenai.annotation.Pk;
import com.example.springaiopenai.annotation.Table;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * Entity đơn hàng, map tới bảng {@code ai_order}
 * </p>
 */
@Data
@Table(name = "ai_order")
public class Order implements Serializable {
    /**
     * Khoá chính tự tăng
     */
    @Pk
    private Long id;

    /**
     * Mã đơn hàng khách hàng nhìn thấy, ví dụ ORD-1001
     */
    @Column(name = "order_code")
    private String orderCode;

    /**
     * Tên khách hàng
     */
    @Column(name = "customer_name")
    private String customerName;

    /**
     * Trạng thái đơn: NEW, PACKING, SHIPPING, DELIVERED, CANCELLED
     */
    private String status;

    /**
     * Vị trí hiện tại của đơn hàng
     */
    private String location;

    /**
     * Mã vận đơn
     */
    @Column(name = "tracking_code")
    private String trackingCode;

    /**
     * Giá trị đơn hàng
     */
    private BigDecimal amount;

    /**
     * Thời điểm tạo
     */
    @Column(name = "create_time")
    private Date createTime;

    /**
     * Thời điểm cập nhật gần nhất
     */
    @Column(name = "last_update_time")
    private Date lastUpdateTime;
}
