package com.younive.store.e2e;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E tests for the order endpoints. Orders can only be created through a successful Stripe
 * checkout (impossible with dummy keys), so these tests focus on the authorization contract:
 * listing requires authentication and returns only the caller's orders; fetching an order requires
 * ownership.
 */
class OrderE2ETest extends BaseApiE2ETest {

    @Test
    @DisplayName("listing orders without a token returns 401")
    void shouldReturn401WhenUnauthenticated() {
        APIResponse response = request.get("/orders");

        assertEquals(401, response.status(), response.text());
    }

    @Test
    @DisplayName("an authenticated user with no orders gets an empty list")
    void shouldReturnEmptyListForNewUser() {
        TestUser user = registerUser();

        APIResponse response = request.get("/orders",
                RequestOptions.create().setHeader("Authorization", "Bearer " + user.token()));

        assertEquals(200, response.status(), response.text());
        var body = json(response);
        assertTrue(body.isArray());
        assertEquals(0, body.size());
    }

    @Test
    @DisplayName("fetching a non-existent order returns 404")
    void shouldReturn404ForMissingOrder() {
        TestUser user = registerUser();

        APIResponse response = request.get("/orders/99999999",
                RequestOptions.create().setHeader("Authorization", "Bearer " + user.token()));

        assertEquals(404, response.status(), response.text());
    }

    @Test
    @DisplayName("fetching an order without a token returns 401")
    void shouldReturn401ForGetOrderWhenUnauthenticated() {
        APIResponse response = request.get("/orders/1");

        assertEquals(401, response.status(), response.text());
    }
}
