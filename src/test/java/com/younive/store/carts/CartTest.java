package com.younive.store.carts;

import com.younive.store.products.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    private Product product(long id, double price) {
        var p = new Product();
        p.setId(id);
        p.setPrice(BigDecimal.valueOf(price));
        return p;
    }

    @Test
    void addItem_newProduct_addsWithQuantityOne() {
        var cart = new Cart();
        var item = cart.addItem(product(1L, 10.0));

        assertThat(item.getQuantity()).isEqualTo(1);
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void addItem_existingProduct_incrementsQuantity() {
        var cart = new Cart();
        var p = product(1L, 10.0);
        cart.addItem(p);
        var item = cart.addItem(p);

        assertThat(item.getQuantity()).isEqualTo(2);
        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void removeItem_removesExistingProduct() {
        var cart = new Cart();
        cart.addItem(product(1L, 10.0));
        cart.removeItem(1L);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    void removeItem_doesNothing_whenProductNotInCart() {
        var cart = new Cart();
        cart.addItem(product(1L, 10.0));
        cart.removeItem(99L);

        assertThat(cart.getItems()).hasSize(1);
    }

    @Test
    void getTotalPrice_sumsPricesAcrossItems() {
        var cart = new Cart();
        cart.addItem(product(1L, 10.0));
        cart.addItem(product(2L, 5.0));

        assertThat(cart.getTotalPrice()).isEqualByComparingTo("15.0");
    }

    @Test
    void getTotalPrice_multiplesQuantity() {
        var cart = new Cart();
        var p = product(1L, 10.0);
        cart.addItem(p);
        cart.addItem(p);

        assertThat(cart.getTotalPrice()).isEqualByComparingTo("20.0");
    }

    @Test
    void clear_removesAllItems() {
        var cart = new Cart();
        cart.addItem(product(1L, 10.0));
        cart.addItem(product(2L, 5.0));
        cart.clear();

        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void isEmpty_trueWhenNoItems() {
        assertThat(new Cart().isEmpty()).isTrue();
    }

    @Test
    void isEmpty_falseWhenHasItems() {
        var cart = new Cart();
        cart.addItem(product(1L, 10.0));

        assertThat(cart.isEmpty()).isFalse();
    }
}
