package com.example.springaiopenai.dao;

import com.example.springaiopenai.dao.base.BaseDao;
import com.example.springaiopenai.entity.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * <p>
 * Order Dao, kế thừa mini ORM {@link BaseDao}
 * </p>
 */
@Repository
public class OrderDao extends BaseDao<Order, Long> {

    public OrderDao(JdbcTemplate jdbcTemplate) {
        super(jdbcTemplate);
    }

    /**
     * Lấy đơn hàng theo mã đơn
     *
     * @param orderCode mã đơn hàng
     * @return đơn hàng, hoặc {@code null} nếu không tồn tại
     */
    public Order selectByOrderCode(String orderCode) {
        String sql = String.format("SELECT * FROM %s WHERE `order_code` = ?", getTableName());
        return findOne(sql, orderCode);
    }

    /**
     * Lấy đơn hàng theo khoá chính
     *
     * @param id khoá chính
     * @return đơn hàng, hoặc {@code null} nếu không tồn tại
     */
    public Order selectById(Long id) {
        return findOneById(id);
    }

    /**
     * Tìm đơn hàng theo tên khách hàng {@code khớp gần đúng}
     *
     * @param customerName tên khách hàng
     * @param limit        số bản ghi tối đa
     * @return danh sách đơn hàng, mới nhất trước
     */
    public List<Order> selectByCustomerName(String customerName, int limit) {
        String sql = String.format(
                "SELECT * FROM %s WHERE `customer_name` LIKE ? ORDER BY `create_time` DESC LIMIT ?",
                getTableName());
        return findList(sql, "%" + customerName + "%", limit);
    }

    /**
     * Đếm số đơn theo trạng thái
     *
     * @param status trạng thái đơn
     * @return số đơn
     */
    public Long countByStatus(String status) {
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE `status` = ?", getTableName());
        return count(sql, status);
    }

    /**
     * Tìm đơn hàng theo entity mẫu
     *
     * @param order điều kiện truy vấn
     * @return danh sách đơn hàng
     */
    public List<Order> selectList(Order order) {
        return findByExample(order);
    }

    /**
     * Thêm đơn hàng
     *
     * @param order đơn hàng
     * @return số dòng bị ảnh hưởng
     */
    public Integer insert(Order order) {
        return super.insert(order, true);
    }

    /**
     * Cập nhật đơn hàng theo khoá chính
     *
     * @param order đơn hàng, field null sẽ được bỏ qua
     * @param id    khoá chính
     * @return số dòng bị ảnh hưởng
     */
    public Integer update(Order order, Long id) {
        return super.updateById(order, id, true);
    }

    /**
     * Xoá đơn hàng theo khoá chính
     *
     * @param id khoá chính
     * @return số dòng bị ảnh hưởng
     */
    public Integer delete(Long id) {
        return super.deleteById(id);
    }
}
