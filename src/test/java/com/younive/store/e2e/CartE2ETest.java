package com.younive.store.e2e;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E tests for the anonymous cart flows (all cart endpoints are permitAll, no auth needed).
 * A product is created once via an admin so items can be added to carts.
 */
class CartE2ETest extends BaseApiE2ETest {

    private static long productId;

    @BeforeAll
    static void createProduct() {
        String adminEmail = uniqueEmail();
        request.post("/users", RequestOptions.create()
                .setData(Map.of("name", "Cart Admin", "email", adminEmail, "password", "password123")));
        AdminSupport.promoteToAdmin(adminEmail);
        APIResponse login = request.post("/auth/login", RequestOptions.create()
                .setData(Map.of("email", adminEmail, "password", "password123")));
        String adminToken;
        try {
            adminToken = MAPPER.readTree(login.body()).get("token").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Cart Test Product");
        body.put("description", "For cart E2E tests");
        body.put("price", 10.00);
        body.put("category_id", null);
        APIResponse created = request.post("/products", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + adminToken)
                .setData(body));
        assertEquals(201, created.status(), created.text());
        try {
            productId = MAPPER.readTree(created.body()).get("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createCart() {
        APIResponse response = request.post("/carts");
        assertEquals(201, response.status(), response.text());
        return json(response).get("id").asText();
    }

    @Test
    @DisplayName("creating a cart returns 201 with a UUID id and empty contents")
    void shouldCreateEmptyCart() {
        APIResponse response = request.post("/carts");

        assertEquals(201, response.status(), response.text());
        var body = json(response);
        assertNotNull(body.get("id").asText());
        assertTrue(body.get("items").isArray());
        assertEquals(0, body.get("items").size());
    }

    @Test
    @DisplayName("adding an item to a cart returns 201 and the item is reflected on the cart")
    void shouldAddItemToCart() {
        String cartId = createCart();

        APIResponse add = request.post("/carts/" + cartId + "/items", RequestOptions.create()
                .setData(Map.of("productId", productId)));
        assertEquals(201, add.status(), add.text());
        var item = json(add);
        assertEquals(productId, item.get("product").get("id").asLong());
        assertEquals(1, item.get("quantity").asInt());

        // Adding the same product again increments quantity to 2.
        APIResponse addAgain = request.post("/carts/" + cartId + "/items", RequestOptions.create()
                .setData(Map.of("productId", productId)));
        assertEquals(201, addAgain.status(), addAgain.text());

        APIResponse cart = request.get("/carts/" + cartId);
        assertEquals(200, cart.status(), cart.text());
        var items = json(cart).get("items");
        assertEquals(1, items.size(), "same product should be a single line item");
        assertEquals(2, items.get(0).get("quantity").asInt());
    }

    @Test
    @DisplayName("adding a non-existent product to a cart returns 400")
    void shouldReturn400WhenProductMissing() {
        String cartId = createCart();

        APIResponse add = request.post("/carts/" + cartId + "/items", RequestOptions.create()
                .setData(Map.of("productId", 99999999L)));

        assertEquals(400, add.status(), add.text());
    }

    @Test
    @DisplayName("getting a non-existent cart returns 404")
    void shouldReturn404ForUnknownCart() {
        APIResponse response = request.get("/carts/" + java.util.UUID.randomUUID());

        assertEquals(404, response.status(), response.text());
    }

    @Test
    @DisplayName("updating an item quantity changes it and recomputes line total")
    void shouldUpdateItemQuantity() {
        String cartId = createCart();
        request.post("/carts/" + cartId + "/items", RequestOptions.create()
                .setData(Map.of("productId", productId)));

        APIResponse update = request.put("/carts/" + cartId + "/items/" + productId,
                RequestOptions.create().setData(Map.of("quantity", 5)));

        assertEquals(200, update.status(), update.text());
        assertEquals(5, json(update).get("quantity").asInt());
    }

    @Test
    @DisplayName("updating with quantity below 1 fails validation (400)")
    void shouldRejectInvalidQuantity() {
        String cartId = createCart();
        request.post("/carts/" + cartId + "/items", RequestOptions.create()
                .setData(Map.of("productId", productId)));

        APIResponse update = request.put("/carts/" + cartId + "/items/" + productId,
                RequestOptions.create().setData(Map.of("quantity", 0)));

        assertEquals(400, update.status(), update.text());
    }

    @Test
    @DisplayName("removing an item returns 204 and the item is gone")
    void shouldRemoveItem() {
        String cartId = createCart();
        request.post("/carts/" + cartId + "/items", RequestOptions.create()
                .setData(Map.of("productId", productId)));

        APIResponse remove = request.delete("/carts/" + cartId + "/items/" + productId);
        assertEquals(204, remove.status(), remove.text());

        APIResponse cart = request.get("/carts/" + cartId);
        assertEquals(0, json(cart).get("items").size());
    }

    @Test
    @DisplayName("clearing a cart returns 204 and empties it")
    void shouldClearCart() {
        String cartId = createCart();
        request.post("/carts/" + cartId + "/items", RequestOptions.create()
                .setData(Map.of("productId", productId)));

        APIResponse clear = request.delete("/carts/" + cartId + "/items");
        assertEquals(204, clear.status(), clear.text());

        APIResponse cart = request.get("/carts/" + cartId);
        assertEquals(0, json(cart).get("items").size());
    }
}
