package com.younive.store.products;

import com.younive.store.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductControllerTest extends BaseIntegrationTest {

    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;

    private Product testProduct;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testCategory = categoryRepository.save(new Category("Electronics"));
        testProduct = productRepository.save(Product.builder()
                .name("Test Laptop")
                .description("A test laptop")
                .price(new BigDecimal("999.99"))
                .category(testCategory)
                .build());
    }

    @Test
    void getAllProducts_returnsArray() throws Exception {
        mockMvc.perform(get("/products")
                        .header("Authorization", testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAllProducts_filterByCategoryId_returnsMatchingProducts() throws Exception {
        mockMvc.perform(get("/products?categoryId=" + testCategory.getId())
                        .header("Authorization", testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Test Laptop')]").exists());
    }

    @Test
    void getAllProducts_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProduct_returnsProduct() throws Exception {
        mockMvc.perform(get("/products/" + testProduct.getId())
                        .header("Authorization", testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Laptop"))
                .andExpect(jsonPath("$.description").value("A test laptop"));
    }

    @Test
    void getProduct_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/products/99999")
                        .header("Authorization", testUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_returns201_withCreatedProduct() throws Exception {
        mockMvc.perform(post("/products")
                        .header("Authorization", testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "New Phone",
                                    "description": "A new phone",
                                    "price": 599.99
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Phone"));
    }

    @Test
    void updateProduct_returnsUpdatedProduct() throws Exception {
        mockMvc.perform(put("/products/" + testProduct.getId())
                        .header("Authorization", testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "id": %d,
                                    "name": "Updated Laptop",
                                    "description": "Updated desc",
                                    "price": 899.99
                                }
                                """.formatted(testProduct.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Updated Laptop"));
    }

    @Test
    void deleteProduct_returns204() throws Exception {
        mockMvc.perform(delete("/products/" + testProduct.getId())
                        .header("Authorization", testUserToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_returns404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/products/99999")
                        .header("Authorization", testUserToken))
                .andExpect(status().isNotFound());
    }
}
