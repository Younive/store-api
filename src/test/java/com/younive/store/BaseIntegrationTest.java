package com.younive.store;

import com.younive.store.auth.JwtService;
import com.younive.store.users.Role;
import com.younive.store.users.User;
import com.younive.store.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired private WebApplicationContext wac;
    @Autowired protected UserRepository userRepository;
    @Autowired protected JwtService jwtService;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected MockMvc mockMvc;
    protected User testUser;
    protected User adminUser;
    protected String testUserToken;
    protected String adminToken;

    @BeforeEach
    void setUpBase() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(springSecurity())
                .build();

        testUser = userRepository.save(User.builder()
                .name("Test User")
                .email("testuser@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.USER)
                .build());
        testUserToken = "Bearer " + jwtService.generateAccessToken(testUser);

        adminUser = userRepository.save(User.builder()
                .name("Admin User")
                .email("admin@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(Role.ADMIN)
                .build());
        adminToken = "Bearer " + jwtService.generateAccessToken(adminUser);
    }
}
