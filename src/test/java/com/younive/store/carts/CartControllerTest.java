package com.younive.store.carts;

import com.younive.store.BaseIntegrationTest;
import com.younive.store.products.Category;
import com.younive.store.products.CategoryRepository;
import com.younive.store.products.Product;
import com.younive.store.products.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CartControllerTest extends BaseIntegrationTest {

    @Autowired CartRepository cartRepository;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;

    private Cart testCart;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        var category = categoryRepository.save(new Category("Test"));
        testProduct = productRepository.save(Product.builder()
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(10.00))
                .category(category)
                .build());
        testCart = cartRepository.save(new Cart());
    }

    @Test
    void createCart_returns201_withId() throws Exception {
        mockMvc.perform(post("/carts"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void getCart_returnsCart() throws Exception {
        mockMvc.perform(get("/carts/" + testCart.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testCart.getId().toString()))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void getCart_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/carts/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void addItemToCart_returns201() throws Exception {
        mockMvc.perform(post("/carts/" + testCart.getId() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productId": %d }
                                """.formatted(testProduct.getId())))
                .andExpect(status().isCreated());
    }

    @Test
    void addItemToCart_returns400_whenProductNotFound() throws Exception {
        mockMvc.perform(post("/carts/" + testCart.getId() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productId": 99999 }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addItemToCart_returns404_whenCartNotFound() throws Exception {
        mockMvc.perform(post("/carts/" + UUID.randomUUID() + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "productId": %d }
                                """.formatted(testProduct.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeCartItem_returns204() throws Exception {
        testCart.addItem(testProduct);
        cartRepository.save(testCart);

        mockMvc.perform(delete("/carts/" + testCart.getId() + "/items/" + testProduct.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void clearCart_returns204() throws Exception {
        testCart.addItem(testProduct);
        cartRepository.save(testCart);

        mockMvc.perform(delete("/carts/" + testCart.getId() + "/items"))
                .andExpect(status().isNoContent());
    }
}
