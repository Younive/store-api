package com.younive.store.e2e;

import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.options.RequestOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E2E tests for the user endpoints: registration, retrieval, update, change-password and the
 * ADMIN-only listing. Authorization boundaries (self-or-admin, admin-only) are exercised here.
 */
class UserE2ETest extends BaseApiE2ETest {

    @Test
    @DisplayName("registering a new user returns 201 with the created user")
    void shouldRegisterNewUser() {
        String email = uniqueEmail();

        APIResponse response = request.post("/users", RequestOptions.create()
                .setData(Map.of("name", "New User", "email", email, "password", "password123")));

        assertEquals(201, response.status(), response.text());
        var body = json(response);
        assertTrue(body.get("id").asLong() > 0);
        assertEquals(email, body.get("email").asText());
        assertEquals("New User", body.get("name").asText());
    }

    @Test
    @DisplayName("registering with a duplicate email returns 400")
    void shouldRejectDuplicateEmail() {
        TestUser existing = registerUser();

        APIResponse response = request.post("/users", RequestOptions.create()
                .setData(Map.of("name", "Dup", "email", existing.email(), "password", "password123")));

        assertEquals(400, response.status(), response.text());
        assertEquals("Email is already registered.", json(response).get("email").asText());
    }

    @Test
    @DisplayName("registering with a too-short password returns 400")
    void shouldRejectShortPassword() {
        APIResponse response = request.post("/users", RequestOptions.create()
                .setData(Map.of("name", "Bad", "email", uniqueEmail(), "password", "123")));

        assertEquals(400, response.status(), response.text());
    }

    @Test
    @DisplayName("GET /users/{id} is allowed for an authenticated user")
    void shouldGetUserByIdWhenAuthenticated() {
        TestUser user = registerUser();

        APIResponse response = request.get("/users/" + user.id(),
                RequestOptions.create().setHeader("Authorization", "Bearer " + user.token()));

        assertEquals(200, response.status(), response.text());
        assertEquals(user.id(), json(response).get("id").asLong());
    }

    @Test
    @DisplayName("GET /users/{id} without a token returns 401")
    void shouldReturn401ForGetUserWhenUnauthenticated() {
        TestUser user = registerUser();

        APIResponse response = request.get("/users/" + user.id());

        assertEquals(401, response.status(), response.text());
    }

    @Test
    @DisplayName("GET /users (list all) is forbidden for a non-admin user")
    void shouldForbidListUsersForRegularUser() {
        TestUser user = registerUser();

        APIResponse response = request.get("/users",
                RequestOptions.create().setHeader("Authorization", "Bearer " + user.token()));

        assertEquals(403, response.status(), response.text());
    }

    @Test
    @DisplayName("a user can change their own password and log in with the new one")
    void shouldAllowSelfPasswordChange() {
        TestUser user = registerUser();
        String newPassword = "newpass456";

        APIResponse change = request.post("/users/" + user.id() + "/change-password",
                RequestOptions.create()
                        .setHeader("Authorization", "Bearer " + user.token())
                        .setData(Map.of("oldPassword", user.password(), "newPassword", newPassword)));

        assertEquals(200, change.status(), change.text());

        // The new password must now work; the old one must not.
        APIResponse newLogin = request.post("/auth/login", RequestOptions.create()
                .setData(Map.of("email", user.email(), "password", newPassword)));
        assertEquals(200, newLogin.status(), newLogin.text());

        APIResponse oldLogin = request.post("/auth/login", RequestOptions.create()
                .setData(Map.of("email", user.email(), "password", user.password())));
        assertEquals(401, oldLogin.status(), oldLogin.text());
    }

    @Test
    @DisplayName("a user cannot change another user's password (403)")
    void shouldForbidChangingAnotherUsersPassword() {
        TestUser victim = registerUser();
        TestUser attacker = registerUser();

        APIResponse response = request.post("/users/" + victim.id() + "/change-password",
                RequestOptions.create()
                        .setHeader("Authorization", "Bearer " + attacker.token())
                        .setData(Map.of("oldPassword", "whatever", "newPassword", "hacked123")));

        assertEquals(403, response.status(), response.text());
    }

    @Test
    @DisplayName("a user can update their own profile")
    void shouldAllowSelfUpdate() {
        TestUser user = registerUser();

        APIResponse response = request.put("/users/" + user.id(), RequestOptions.create()
                .setHeader("Authorization", "Bearer " + user.token())
                .setData(Map.of("name", "Updated Name", "email", user.email())));

        assertEquals(200, response.status(), response.text());
        assertEquals("Updated Name", json(response).get("name").asText());
    }

    @Test
    @DisplayName("a user cannot update another user (403)")
    void shouldForbidUpdatingAnotherUser() {
        TestUser victim = registerUser();
        TestUser attacker = registerUser();

        APIResponse response = request.put("/users/" + victim.id(), RequestOptions.create()
                .setHeader("Authorization", "Bearer " + attacker.token())
                .setData(Map.of("name", "Hacked", "email", victim.email())));

        assertEquals(403, response.status(), response.text());
    }
}
