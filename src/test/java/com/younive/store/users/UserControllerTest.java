package com.younive.store.users;

import com.younive.store.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends BaseIntegrationTest {

    @Test
    void registerUser_returns201_withUserDto() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Alice",
                                    "email": "alice@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void registerUser_returns400_whenEmailAlreadyExists() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Duplicate",
                                    "email": "testuser@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email is already registered."));
    }

    @Test
    void registerUser_returns400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "",
                                    "email": "valid@example.com",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void registerUser_returns400_whenEmailInvalid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Alice",
                                    "email": "not-an-email",
                                    "password": "password123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void registerUser_returns400_whenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Alice",
                                    "email": "alice@example.com",
                                    "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void getUser_returnsUser_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/users/" + testUser.getId())
                        .header("Authorization", testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test User"));
    }

    @Test
    void getUser_returns401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/users/" + testUser.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUser_returns404_whenUserNotFound() throws Exception {
        mockMvc.perform(get("/users/99999")
                        .header("Authorization", testUserToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_returnsUpdatedUser() throws Exception {
        mockMvc.perform(put("/users/" + testUser.getId())
                        .header("Authorization", testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Updated Name" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }
}
