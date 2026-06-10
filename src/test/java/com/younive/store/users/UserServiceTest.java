package com.younive.store.users;

import com.younive.store.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserMapper userMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthService authService;
    @InjectMocks UserService userService;

    @Test
    void registerUser_success() {
        var request = new RegisterUserRequest();
        request.setName("Alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        var user = User.builder().id(1L).name("Alice").email("alice@example.com").build();
        var dto = new UserDto(1L, "Alice", "alice@example.com");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userMapper.toDto(user)).thenReturn(dto);

        var result = userService.registerUser(request);

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        verify(userRepository).save(user);
    }

    @Test
    void registerUser_throwsDuplicateUserException_whenEmailExists() {
        var request = new RegisterUserRequest();
        request.setEmail("existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(request))
                .isInstanceOf(DuplicateUserException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUser_returnsDto() {
        var user = User.builder().id(1L).name("Alice").build();
        var dto = new UserDto(1L, "Alice", "alice@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        assertThat(userService.getUser(1L).getId()).isEqualTo(1L);
    }

    @Test
    void getUser_throwsUserNotFoundException_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateUser_savesAndReturnsDto() {
        var user = User.builder().id(1L).name("Old").role(Role.USER).build();
        var request = new UpdateUserRequest();
        request.setName("New");
        var dto = new UserDto(1L, "New", "user@example.com");

        when(authService.getCurrentUser()).thenReturn(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        var result = userService.updateUser(1L, request);

        verify(userMapper).update(request, user);
        verify(userRepository).save(user);
        assertThat(result.getName()).isEqualTo("New");
    }

    @Test
    void updateUser_throwsAccessDeniedException_whenNotOwnerNorAdmin() {
        var currentUser = User.builder().id(2L).role(Role.USER).build();
        when(authService.getCurrentUser()).thenReturn(currentUser);

        assertThatThrownBy(() -> userService.updateUser(1L, new UpdateUserRequest()))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_allowsAdmin_toUpdateOtherUser() {
        var admin = User.builder().id(2L).role(Role.ADMIN).build();
        var user = User.builder().id(1L).name("Old").role(Role.USER).build();
        var request = new UpdateUserRequest();
        var dto = new UserDto(1L, "New", "user@example.com");

        when(authService.getCurrentUser()).thenReturn(admin);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(dto);

        userService.updateUser(1L, request);

        verify(userRepository).save(user);
    }

    @Test
    void deleteUser_callsRepositoryDelete() {
        var user = User.builder().id(1L).role(Role.USER).build();
        when(authService.getCurrentUser()).thenReturn(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_throwsAccessDeniedException_whenNotOwnerNorAdmin() {
        var currentUser = User.builder().id(2L).role(Role.USER).build();
        when(authService.getCurrentUser()).thenReturn(currentUser);

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(AccessDeniedException.class);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void changePassword_updatesPasswordEncoded() {
        var user = User.builder().id(1L).password("hashed_old").role(Role.USER).build();
        var request = new ChangePasswordRequest();
        request.setOldPassword("old_pass");
        request.setNewPassword("new_pass");

        when(authService.getCurrentUser()).thenReturn(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old_pass", "hashed_old")).thenReturn(true);
        when(passwordEncoder.encode("new_pass")).thenReturn("hashed_new");

        userService.changePassword(1L, request);

        assertThat(user.getPassword()).isEqualTo("hashed_new");
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_throwsAccessDeniedException_whenWrongOldPassword() {
        var user = User.builder().id(1L).password("hashed_old").role(Role.USER).build();
        var request = new ChangePasswordRequest();
        request.setOldPassword("wrong");
        request.setNewPassword("new_pass");

        when(authService.getCurrentUser()).thenReturn(user);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed_old")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1L, request))
                .isInstanceOf(AccessDeniedException.class);
    }
}
