package com.younive.store.carts;

import com.younive.store.products.Product;
import com.younive.store.products.ProductNotFoundException;
import com.younive.store.products.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;
    @Mock CartMapper cartMapper;
    @InjectMocks CartService cartService;

    private Product mockProduct(long id) {
        var p = new Product();
        p.setId(id);
        p.setPrice(BigDecimal.TEN);
        return p;
    }

    @Test
    void createCart_savesCartAndReturnsDto() {
        var dto = new CartDto();
        dto.setId(UUID.randomUUID());
        when(cartMapper.toDto(any(Cart.class))).thenReturn(dto);

        var result = cartService.createCart();

        verify(cartRepository).save(any(Cart.class));
        assertThat(result.getId()).isNotNull();
    }

    @Test
    void addToCart_success() {
        var cartId = UUID.randomUUID();
        var product = mockProduct(1L);
        var cart = new Cart();
        var dto = new CartItemDto();

        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartMapper.toDto(any(CartItem.class))).thenReturn(dto);

        var result = cartService.addToCart(cartId, 1L);

        verify(cartRepository).save(cart);
        assertThat(result).isNotNull();
    }

    @Test
    void addToCart_throwsCartNotFoundException_whenCartMissing() {
        var cartId = UUID.randomUUID();
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(cartId, 1L))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void addToCart_throwsProductNotFoundException_whenProductMissing() {
        var cartId = UUID.randomUUID();
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(new Cart()));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(cartId, 99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getCart_returnsDto() {
        var cartId = UUID.randomUUID();
        var cart = new Cart();
        var dto = new CartDto();

        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(cart)).thenReturn(dto);

        assertThat(cartService.getCart(cartId)).isEqualTo(dto);
    }

    @Test
    void getCart_throwsCartNotFoundException_whenMissing() {
        var cartId = UUID.randomUUID();
        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getCart(cartId))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void updateItem_updatesQuantityAndReturnsDto() {
        var cartId = UUID.randomUUID();
        var product = mockProduct(1L);
        var cart = new Cart();
        cart.addItem(product);
        var dto = new CartItemDto();

        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));
        when(cartMapper.toDto(any(CartItem.class))).thenReturn(dto);

        cartService.updateItem(cartId, 1L, 5);

        assertThat(cart.getItem(1L).getQuantity()).isEqualTo(5);
        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_removesAllItems() {
        var cartId = UUID.randomUUID();
        var product = mockProduct(1L);
        var cart = new Cart();
        cart.addItem(product);

        when(cartRepository.getCartWithItems(cartId)).thenReturn(Optional.of(cart));

        cartService.clearCart(cartId);

        assertThat(cart.isEmpty()).isTrue();
        verify(cartRepository).save(cart);
    }
}
