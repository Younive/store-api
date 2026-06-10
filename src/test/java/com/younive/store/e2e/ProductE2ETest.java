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
 * E2E tests for the product endpoints. Reads are authenticated; writes (POST/PUT/DELETE) are
 * ADMIN-only. Admin access is obtained by registering a user and promoting it via JDBC, since the
 * application seeds no admin and registration always assigns role USER.
 *
 * <p>Products are created with a null category ({@code category_id}) because no categories are
 * seeded; the products.category_id column is nullable.
 */
class ProductE2ETest extends BaseApiE2ETest {

    private static String adminToken;
    private static String userToken;

    @BeforeAll
    static void setUpActors() {
        // A regular user for read + forbidden-write checks.
        String userEmail = uniqueEmail();
        request.post("/users", RequestOptions.create()
                .setData(Map.of("name", "Reader", "email", userEmail, "password", "password123")));
        userToken = loginToken(userEmail, "password123");

        // An admin: register then promote in MySQL.
        String adminEmail = uniqueEmail();
        request.post("/users", RequestOptions.create()
                .setData(Map.of("name", "Prod Admin", "email", adminEmail, "password", "password123")));
        AdminSupport.promoteToAdmin(adminEmail);
        adminToken = loginToken(adminEmail, "password123");
    }

    private static String loginToken(String email, String password) {
        APIResponse response = request.post("/auth/login", RequestOptions.create()
                .setData(Map.of("email", email, "password", password)));
        assertEquals(200, response.status(), response.text());
        try {
            return MAPPER.readTree(response.body()).get("token").asText();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Builds a product create body. category_id is sent as null (no seeded categories). */
    private static Map<String, Object> productBody(String name, double price) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("description", "Created by E2E test");
        body.put("price", price);
        body.put("category_id", null);
        return body;
    }

    @Test
    @DisplayName("an admin can create a product (201)")
    void shouldCreateProductAsAdmin() {
        APIResponse response = request.post("/products", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + adminToken)
                .setData(productBody("E2E Widget", 19.99)));

        assertEquals(201, response.status(), response.text());
        var body = json(response);
        assertTrue(body.get("id").asLong() > 0);
        assertEquals("E2E Widget", body.get("name").asText());
        assertEquals(19.99, body.get("price").asDouble(), 0.001);
    }

    @Test
    @DisplayName("a non-admin user cannot create a product (403)")
    void shouldForbidProductCreationForRegularUser() {
        APIResponse response = request.post("/products", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + userToken)
                .setData(productBody("Forbidden Widget", 5.0)));

        assertEquals(403, response.status(), response.text());
    }

    @Test
    @DisplayName("creating a product without a token returns 401")
    void shouldRejectProductCreationWhenUnauthenticated() {
        APIResponse response = request.post("/products", RequestOptions.create()
                .setData(productBody("Anon Widget", 5.0)));

        assertEquals(401, response.status(), response.text());
    }

    @Test
    @DisplayName("an authenticated user can list products and read one by id")
    void shouldListAndGetProducts() {
        // Arrange: admin creates a product.
        APIResponse created = request.post("/products", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + adminToken)
                .setData(productBody("Readable Widget", 12.50)));
        assertEquals(201, created.status(), created.text());
        long id = json(created).get("id").asLong();

        // Act + Assert: regular user lists and fetches it.
        APIResponse list = request.get("/products",
                RequestOptions.create().setHeader("Authorization", "Bearer " + userToken));
        assertEquals(200, list.status(), list.text());
        assertTrue(json(list).isArray());

        APIResponse one = request.get("/products/" + id,
                RequestOptions.create().setHeader("Authorization", "Bearer " + userToken));
        assertEquals(200, one.status(), one.text());
        assertEquals(id, json(one).get("id").asLong());
    }

    @Test
    @DisplayName("listing products without a token returns 401")
    void shouldRejectListProductsWhenUnauthenticated() {
        APIResponse response = request.get("/products");

        assertEquals(401, response.status(), response.text());
    }

    @Test
    @DisplayName("GET /products/{id} for a missing product returns 404")
    void shouldReturn404ForMissingProduct() {
        APIResponse response = request.get("/products/99999999",
                RequestOptions.create().setHeader("Authorization", "Bearer " + userToken));

        assertEquals(404, response.status(), response.text());
    }

    @Test
    @DisplayName("an admin can update and then delete a product")
    void shouldUpdateAndDeleteProductAsAdmin() {
        // Create
        APIResponse created = request.post("/products", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + adminToken)
                .setData(productBody("Mutable Widget", 1.0)));
        long id = json(created).get("id").asLong();

        // Update
        Map<String, Object> update = productBody("Renamed Widget", 2.5);
        APIResponse updated = request.put("/products/" + id, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + adminToken)
                .setData(update));
        assertEquals(200, updated.status(), updated.text());
        assertEquals("Renamed Widget", json(updated).get("name").asText());

        // Delete
        APIResponse deleted = request.delete("/products/" + id, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + adminToken));
        assertEquals(204, deleted.status(), deleted.text());

        // Gone
        APIResponse afterDelete = request.get("/products/" + id, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + adminToken));
        assertEquals(404, afterDelete.status(), afterDelete.text());
    }

    @Test
    @DisplayName("a non-admin user cannot delete a product (403)")
    void shouldForbidDeleteForRegularUser() {
        APIResponse created = request.post("/products", RequestOptions.create()
                .setHeader("Authorization", "Bearer " + adminToken)
                .setData(productBody("Protected Widget", 9.0)));
        long id = json(created).get("id").asLong();

        APIResponse response = request.delete("/products/" + id, RequestOptions.create()
                .setHeader("Authorization", "Bearer " + userToken));

        assertEquals(403, response.status(), response.text());
        assertNotNull(adminToken); // sanity that setup ran
    }
}
