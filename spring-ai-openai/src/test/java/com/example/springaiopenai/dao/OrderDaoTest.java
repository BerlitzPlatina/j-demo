package com.example.springaiopenai.dao;

import com.example.springaiopenai.entity.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>
 * Kiểm tra SQL do mini ORM sinh ra, JdbcTemplate được mock nên không cần database
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class OrderDaoTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @SuppressWarnings("unchecked")
    private void stubQueryResult(List<Order> result) {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(result);
    }

    @Test
    void insert_shouldBuildInsertSqlWithMappedColumnsAndSkipNullAndAutoPk() {
        OrderDao orderDao = new OrderDao(jdbcTemplate);
        Order order = new Order();
        order.setId(999L); // khoá tự tăng, phải bị loại khỏi câu INSERT
        order.setOrderCode("ORD-2001");
        order.setCustomerName("Test User");
        order.setStatus("NEW");

        orderDao.insert(order);

        verify(jdbcTemplate).update(
                "INSERT INTO `ai_order` (`order_code`,`customer_name`,`status`) VALUES (?,?,?)",
                "ORD-2001", "Test User", "NEW");
    }

    @Test
    void update_shouldOnlySetNonNullColumnsAndFilterByPk() {
        OrderDao orderDao = new OrderDao(jdbcTemplate);
        Order order = new Order();
        order.setStatus("CANCELLED");
        Date now = new Date();
        order.setLastUpdateTime(now);

        orderDao.update(order, 7L);

        verify(jdbcTemplate).update(
                "UPDATE `ai_order` SET `status` = ?,`last_update_time` = ? WHERE `id` = ?",
                "CANCELLED", now, 7L);
    }

    @Test
    void selectByOrderCode_shouldReturnNullWhenNoRowMatches() {
        stubQueryResult(List.of());
        OrderDao orderDao = new OrderDao(jdbcTemplate);

        assertThat(orderDao.selectByOrderCode("ORD-9999")).isNull();

        verify(jdbcTemplate).query(
                eq("SELECT * FROM `ai_order` WHERE `order_code` = ?"),
                any(RowMapper.class),
                eq("ORD-9999"));
    }

    @Test
    void selectByCustomerName_shouldUseLikeAndLimit() {
        Order found = new Order();
        found.setOrderCode("ORD-1001");
        found.setAmount(new BigDecimal("1250000.00"));
        stubQueryResult(List.of(found));
        OrderDao orderDao = new OrderDao(jdbcTemplate);

        List<Order> orders = orderDao.selectByCustomerName("An", 5);

        assertThat(orders).hasSize(1);
        verify(jdbcTemplate).query(
                eq("SELECT * FROM `ai_order` WHERE `customer_name` LIKE ? ORDER BY `create_time` DESC LIMIT ?"),
                any(RowMapper.class),
                eq("%An%"), eq(5));
    }

    @Test
    void findByExample_shouldJoinNonNullFieldsWithAnd() {
        stubQueryResult(List.of());
        OrderDao orderDao = new OrderDao(jdbcTemplate);
        Order example = new Order();
        example.setStatus("SHIPPING");
        example.setLocation("Hà Nội");

        orderDao.selectList(example);

        verify(jdbcTemplate).query(
                eq("SELECT * FROM `ai_order` WHERE 1=1  AND `status` = ?  AND `location` = ? "),
                any(RowMapper.class),
                eq("SHIPPING"), eq("Hà Nội"));
    }

    @Test
    void countByStatus_shouldReturnZeroWhenQueryReturnsNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), any(Object[].class))).thenReturn(null);
        OrderDao orderDao = new OrderDao(jdbcTemplate);

        assertThat(orderDao.countByStatus("NEW")).isZero();

        verify(jdbcTemplate).queryForObject(
                eq("SELECT COUNT(*) FROM `ai_order` WHERE `status` = ?"),
                eq(Long.class),
                eq("NEW"));
    }
}
