package com.younive.store.e2e;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E tests for the checkout + webhook contract.
 *
 * <p>POST /checkout calls the real Stripe API. With the dummy STRIPE_SECRET_KEY used in non-payment
 * environments the gateway throws, which the controller maps to 500 with the
 * {"error":"Error creating a checkout session"} body. We assert that error contract rather than a
 * happy path (a real checkout would need valid Stripe keys).
 *
 * <p>The webhook endpoint is permitAll; a forged or missing Stripe signature is mapped to
 * PaymentException and the controller returns 400 so Stripe does not retry.
 */
class CheckoutE2ETest extends BaseApiE2ETest {

    private static long productId;

    @BeforeAll
    static void createProduct() {
        String adminEmail = uniqueEmail();
        request.post("/users", RequestOptions.create()
                .setData(Map.of("name", "Checkout Admin", "email", adminEmail, "password", "password123")));
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
        body.put("name", "Checkout Product");
        body.put("description", "For checkout E2E tests");
        body.put("price", 25.00);
        body.put("category_id", null);
        APIResponse created = request.post("/products", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + adminToken)
                .setData(body));
        try {
            productId = MAPPER.readTree(created.body()).get("id").asLong();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("checkout without a token returns 401")
    void shouldReturn401WhenUnauthenticated() {
        APIResponse response = request.post("/checkout", RequestOptions.create()
                .setData(Map.of("cartId", UUID.randomUUID().toString())));

        assertEquals(401, response.status(), response.text());
    }

    @Test
    @DisplayName("checkout of a non-existent cart returns 400 (cart not found)")
    void shouldReturn400ForUnknownCart() {
        TestUser user = registerUser();

        APIResponse response = request.post("/checkout", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + user.token())
                .setData(Map.of("cartId", UUID.randomUUID().toString())));

        assertEquals(400, response.status(), response.text());
    }

    @Test
    @DisplayName("checkout of a valid cart fails at the Stripe call and returns the 500 error contract")
    void shouldReturn500FromStripeWithDummyKeys() {
        // Arrange: a cart with one real item, owned-agnostic (carts are anonymous).
        TestUser user = registerUser();
        APIResponse cart = request.post("/carts");
        String cartId = json(cart).get("id").asText();
        request.post("/carts/" + cartId + "/items", RequestOptions.create()
                .setData(Map.of("productId", productId)));

        // Act
        APIResponse response = request.post("/checkout", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + user.token())
                .setData(Map.of("cartId", cartId)));

        // Assert: with dummy Stripe keys the gateway throws -> 500 PaymentException contract.
        assertEquals(500, response.status(), response.text());
        assertEquals("Error creating a checkout session", json(response).get("error").asText());
    }

    @Test
    @DisplayName("webhook with an invalid signature is rejected (400)")
    void shouldRejectWebhookWithBadSignature() {
        // Invalid signature -> Stripe verification throws -> PaymentException -> controller
        // returns 400 (a forged webhook is rejected, never processed, and Stripe won't retry).
        APIResponse response = request.post("/checkout/webhook", RequestOptions.create()
                .setHeader("Stripe-Signature", "t=0,v1=deadbeef")
                .setData("{\"id\":\"evt_test\",\"type\":\"checkout.session.completed\"}"));

        assertEquals(400, response.status(), response.text());
    }

    @Test
    @DisplayName("webhook without a Stripe-Signature header is rejected (400)")
    void shouldRejectWebhookWithoutSignature() {
        APIResponse response = request.post("/checkout/webhook", RequestOptions.create()
                .setData("{\"id\":\"evt_test\",\"type\":\"checkout.session.completed\"}"));

        assertEquals(400, response.status(), response.text());
    }
}
