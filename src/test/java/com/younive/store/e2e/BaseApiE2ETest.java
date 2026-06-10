package com.younive.store.e2e;

import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class for the Playwright-Java E2E API tests.
 *
 * <p>Because the API has no meaningful browser UI (only a trivial Thymeleaf index page), these
 * tests drive the REST endpoints through Playwright's {@link APIRequestContext} rather than a
 * browser. A single {@link Playwright} instance and {@link APIRequestContext} are created once per
 * test class (cheaper than per-test) and disposed afterwards.
 *
 * <p>All tests are tagged {@code "e2e"} and live in the {@code com.younive.store.e2e} package, both
 * of which keep them out of the default {@code mvn test} run (see the surefire config + {@code e2e}
 * profile in pom.xml). They require a running server and MySQL.
 */
@Tag("e2e")
abstract class BaseApiE2ETest {

    protected static Playwright playwright;
    protected static APIRequestContext request;
    protected static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void launch() {
        playwright = Playwright.create();
        request = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(E2EConfig.baseUrl()));
    }

    @AfterAll
    static void shutdown() {
        if (request != null) {
            request.dispose();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Generates a unique email so reruns don't collide with rows already persisted in MySQL. */
    protected static String uniqueEmail() {
        return "e2e-" + UUID.randomUUID() + "@example.com";
    }

    protected static JsonNode json(APIResponse response) {
        try {
            return MAPPER.readTree(response.body());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response: " + response.text(), e);
        }
    }

    /** Bearer-auth header map for an access token. */
    protected static Map<String, String> bearer(String token) {
        return Map.of("Authorization", "Bearer " + token);
    }

    /**
     * Registers a brand-new USER and returns {id, email, password, token}.
     * Registration always assigns role USER (no public way to create an admin).
     */
    protected TestUser registerUser() {
        String email = uniqueEmail();
        String password = "password123";
        APIResponse create = request.post("/users", RequestOptions.create()
                .setData(Map.of("name", "E2E User", "email", email, "password", password)));
        assertEquals(201, create.status(), "user registration should return 201: " + create.text());
        long id = json(create).get("id").asLong();
        String token = login(email, password);
        return new TestUser(id, email, password, token);
    }

    /** Logs in and returns the access token (the {token} field of JwtResponse). */
    protected String login(String email, String password) {
        APIResponse response = request.post("/auth/login", RequestOptions.create()
                .setData(Map.of("email", email, "password", password)));
        if (response.status() != 200) {
            fail("login failed (" + response.status() + "): " + response.text());
        }
        return json(response).get("token").asText();
    }

    /** Simple holder for a registered test user. */
    protected record TestUser(long id, String email, String password, String token) {
    }
}
