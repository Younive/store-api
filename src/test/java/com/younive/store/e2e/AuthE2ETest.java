package com.younive.store.e2e;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E tests for the authentication flows: POST /auth/login, POST /auth/refresh, GET /auth/me.
 */
class AuthE2ETest extends BaseApiE2ETest {

    @Test
    @DisplayName("login with valid credentials returns 200 and an access token")
    void shouldReturnTokenWhenLoginSucceeds() {
        TestUser user = registerUser();

        APIResponse response = request.post("/auth/login", RequestOptions.create()
                .setData(Map.of("email", user.email(), "password", user.password())));

        assertEquals(200, response.status(), response.text());
        String token = json(response).get("token").asText();
        assertFalse(token.isBlank(), "token should not be blank");
    }

    @Test
    @DisplayName("login with a wrong password returns 401")
    void shouldReturn401WhenPasswordIsInvalid() {
        TestUser user = registerUser();

        APIResponse response = request.post("/auth/login", RequestOptions.create()
                .setData(Map.of("email", user.email(), "password", "wrong-password")));

        assertEquals(401, response.status(), response.text());
    }

    @Test
    @DisplayName("login with an unknown email returns 401")
    void shouldReturn401WhenUserDoesNotExist() {
        APIResponse response = request.post("/auth/login", RequestOptions.create()
                .setData(Map.of("email", uniqueEmail(), "password", "password123")));

        assertEquals(401, response.status(), response.text());
    }

    @Test
    @DisplayName("GET /auth/me with a valid Bearer token returns the current user")
    void shouldReturnCurrentUserWhenAuthenticated() {
        TestUser user = registerUser();

        APIResponse response = request.get("/auth/me", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + user.token()));

        assertEquals(200, response.status(), response.text());
        var body = json(response);
        assertEquals(user.id(), body.get("id").asLong());
        assertEquals(user.email(), body.get("email").asText());
        assertTrue(body.has("name"));
    }

    @Test
    @DisplayName("GET /auth/me without a token returns 401")
    void shouldReturn401ForMeWhenUnauthenticated() {
        APIResponse response = request.get("/auth/me");

        assertEquals(401, response.status(), response.text());
    }

    @Test
    @DisplayName("refresh rejects an access token (wrong token type)")
    void shouldRejectAccessTokenOnRefresh() {
        // The refresh endpoint reads the refreshToken cookie. Feeding it an access token must be
        // rejected because of the token "type" claim: the service throws BadCredentialsException,
        // which the controller's exception handler maps to 401.
        TestUser user = registerUser();

        APIResponse response = request.post("/auth/refresh", RequestOptions.create()
                .setHeader("Cookie", "refreshToken=" + user.token()));

        assertEquals(401, response.status(), response.text());
    }

    @Test
    @DisplayName("refresh without a refreshToken cookie is rejected (400)")
    void shouldRejectRefreshWhenCookieMissing() {
        // @CookieValue without a default makes the cookie required; the resulting
        // MissingRequestCookieException is a client error, and with the ERROR dispatch
        // permitted in the security config the proper 400 status reaches the client.
        //
        // Use a throwaway request context so cookies persisted by other tests in the shared
        // class-level context can't leak a real refreshToken into this call.
        var cleanContext = playwright.request().newContext(
                new com.microsoft.playwright.APIRequest.NewContextOptions()
                        .setBaseURL(E2EConfig.baseUrl()));
        try {
            APIResponse response = cleanContext.post("/auth/refresh");
            assertEquals(400, response.status(), response.text());
        } finally {
            cleanContext.dispose();
        }
    }
}
