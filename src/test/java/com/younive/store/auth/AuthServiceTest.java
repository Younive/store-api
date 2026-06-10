package com.younive.store.auth;

import com.younive.store.users.Role;
import com.younive.store.users.User;
import com.younive.store.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    @Test
    void login_success() {
        var request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password");

        var user = User.builder().id(1L).email("user@example.com").role(Role.USER).build();
        var accessToken = mock(Jwt.class);
        var refreshToken = mock(Jwt.class);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn(accessToken);
        when(jwtService.generateRefreshToken(user)).thenReturn(refreshToken);

        var result = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(result.getAccessToken()).isEqualTo(accessToken);
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
    }

    @Test
    void login_propagatesBadCredentialsException() {
        var request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshAccessToken_returnsNewToken() {
        var user = User.builder().id(1L).role(Role.USER).build();
        var jwt = mock(Jwt.class);
        var newToken = mock(Jwt.class);

        when(jwtService.parseToken("valid_token")).thenReturn(jwt);
        when(jwt.isExpired()).thenReturn(false);
        when(jwt.isRefreshToken()).thenReturn(true);
        when(jwt.getUserId()).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn(newToken);

        assertThat(authService.refreshAccessToken("valid_token")).isEqualTo(newToken);
    }

    @Test
    void refreshAccessToken_throwsBadCredentials_whenAccessTokenUsed() {
        var jwt = mock(Jwt.class);
        when(jwtService.parseToken("access_token")).thenReturn(jwt);
        when(jwt.isExpired()).thenReturn(false);
        when(jwt.isRefreshToken()).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshAccessToken("access_token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshAccessToken_throwsBadCredentials_whenTokenNull() {
        when(jwtService.parseToken("bad_token")).thenReturn(null);

        assertThatThrownBy(() -> authService.refreshAccessToken("bad_token"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void refreshAccessToken_throwsBadCredentials_whenTokenExpired() {
        var jwt = mock(Jwt.class);
        when(jwtService.parseToken("expired")).thenReturn(jwt);
        when(jwt.isExpired()).thenReturn(true);

        assertThatThrownBy(() -> authService.refreshAccessToken("expired"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
