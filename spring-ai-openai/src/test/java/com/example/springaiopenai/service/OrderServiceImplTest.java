package com.example.springaiopenai.service;

import com.example.springaiopenai.dao.OrderDao;
import com.example.springaiopenai.entity.Order;
import com.example.springaiopenai.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @InjectMocks
    private OrderServiceImpl orderService;

    private static Order order(Long id, String code, String status) {
        Order order = new Order();
        order.setId(id);
        order.setOrderCode(code);
        order.setStatus(status);
        return order;
    }

    @Test
    void getByOrderCode_shouldTrimCodeBeforeQuery() {
        Order exist = order(1L, "ORD-1001", "SHIPPING");
        when(orderDao.selectByOrderCode("ORD-1001")).thenReturn(exist);

        assertThat(orderService.getByOrderCode("  ORD-1001 ")).isSameAs(exist);
    }

    @Test
    void getByOrderCode_shouldRejectBlankCode() {
        assertThatThrownBy(() -> orderService.getByOrderCode(" "))
                .isInstanceOf(IllegalArgumentException.class);
        verify(orderDao, never()).selectByOrderCode(anyString());
    }

    @Test
    void findByCustomerName_shouldFallbackToDefaultLimitAndCapAt50() {
        when(orderDao.selectByCustomerName(anyString(), anyInt())).thenReturn(List.of());

        orderService.findByCustomerName("An", 0);
        verify(orderDao).selectByCustomerName("An", 5);

        orderService.findByCustomerName("An", 999);
        verify(orderDao).selectByCustomerName("An", 50);
    }

    @Test
    void countByStatus_shouldNormalizeStatusToUpperCase() {
        when(orderDao.countByStatus("SHIPPING")).thenReturn(3L);

        assertThat(orderService.countByStatus(" shipping ")).isEqualTo(3L);
    }

    @Test
    void cancelOrder_shouldUpdateOnlyStatusAndTimestamp() {
        Order exist = order(4L, "ORD-1004", "NEW");
        Order cancelled = order(4L, "ORD-1004", "CANCELLED");
        when(orderDao.selectByOrderCode("ORD-1004")).thenReturn(exist);
        when(orderDao.selectById(4L)).thenReturn(cancelled);

        assertThat(orderService.cancelOrder("ORD-1004")).isSameAs(cancelled);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderDao).update(captor.capture(), eq(4L));
        Order patch = captor.getValue();
        assertThat(patch.getStatus()).isEqualTo("CANCELLED");
        assertThat(patch.getLastUpdateTime()).isNotNull();
        assertThat(patch.getOrderCode()).isNull();
        assertThat(patch.getCustomerName()).isNull();
    }

    @Test
    void cancelOrder_shouldFailWhenOrderNotFound() {
        when(orderDao.selectByOrderCode("ORD-9999")).thenReturn(null);

        assertThatThrownBy(() -> orderService.cancelOrder("ORD-9999"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không tìm thấy đơn hàng");
    }

    @Test
    void cancelOrder_shouldFailWhenOrderAlreadyDelivered() {
        when(orderDao.selectByOrderCode("ORD-1003")).thenReturn(order(3L, "ORD-1003", "DELIVERED"));

        assertThatThrownBy(() -> orderService.cancelOrder("ORD-1003"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã giao thành công");
        verify(orderDao, never()).update(any(), anyLong());
    }

    @Test
    void cancelOrder_shouldBeIdempotentWhenAlreadyCancelled() {
        Order exist = order(5L, "ORD-1005", "CANCELLED");
        when(orderDao.selectByOrderCode("ORD-1005")).thenReturn(exist);

        assertThat(orderService.cancelOrder("ORD-1005")).isSameAs(exist);
        verify(orderDao, never()).update(any(), anyLong());
    }
}
