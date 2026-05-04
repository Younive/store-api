package com.younive.store.auth;

import com.younive.store.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends BaseIntegrationTest {

    @Test
    void login_returnsToken_whenCredentialsValid() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "testuser@example.com", "password": "password123" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_returns401_whenPasswordWrong() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "testuser@example.com", "password": "wrongpassword" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns401_whenEmailNotFound() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "email": "nobody@example.com", "password": "password123" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returnsCurrentUser_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/auth/me")
                        .header("Authorization", testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void me_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
