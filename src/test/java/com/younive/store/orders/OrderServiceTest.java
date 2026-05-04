package com.younive.store.orders;

import com.younive.store.auth.AuthService;
import com.younive.store.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock AuthService authService;
    @Mock OrderRepository orderRepository;
    @Mock OrderMapper orderMapper;
    @InjectMocks OrderService orderService;

    private User user(long id) {
        return User.builder().id(id).build();
    }

    @Test
    void getAllOrders_returnsCurrentUserOrders() {
        var user = user(1L);
        var order = new Order();
        var dto = new OrderDto();

        when(authService.getCurrentUser()).thenReturn(user);
        when(orderRepository.getOrderByCustomer(user)).thenReturn(List.of(order));
        when(orderMapper.toDto(order)).thenReturn(dto);

        var result = orderService.getAllOrders();

        assertThat(result).hasSize(1).contains(dto);
    }

    @Test
    void getAllOrders_returnsEmptyList_whenNoOrders() {
        var user = user(1L);
        when(authService.getCurrentUser()).thenReturn(user);
        when(orderRepository.getOrderByCustomer(user)).thenReturn(List.of());

        assertThat(orderService.getAllOrders()).isEmpty();
    }

    @Test
    void getOrder_returnsDto_whenOwnedByCurrentUser() {
        var user = user(1L);
        var order = new Order();
        order.setCustomer(user);
        var dto = new OrderDto();

        when(orderRepository.getOrderWithItems(1L)).thenReturn(Optional.of(order));
        when(authService.getCurrentUser()).thenReturn(user);
        when(orderMapper.toDto(order)).thenReturn(dto);

        assertThat(orderService.getOrder(1L)).isEqualTo(dto);
    }

    @Test
    void getOrder_throwsOrderNotFoundException_whenMissing() {
        when(orderRepository.getOrderWithItems(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(99L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrder_throwsAccessDeniedException_whenNotOwner() {
        var owner = user(1L);
        var other = user(2L);
        var order = new Order();
        order.setCustomer(owner);

        when(orderRepository.getOrderWithItems(1L)).thenReturn(Optional.of(order));
        when(authService.getCurrentUser()).thenReturn(other);

        assertThatThrownBy(() -> orderService.getOrder(1L))
                .isInstanceOf(AccessDeniedException.class);
    }
}
