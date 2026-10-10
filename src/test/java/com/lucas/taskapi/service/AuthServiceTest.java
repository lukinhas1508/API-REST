package com.lucas.taskapi.service;

import com.lucas.taskapi.dto.auth.LoginRequest;
import com.lucas.taskapi.dto.auth.RegisterRequest;
import com.lucas.taskapi.dto.auth.TokenResponse;
import com.lucas.taskapi.dto.auth.UserResponse;
import com.lucas.taskapi.entity.User;
import com.lucas.taskapi.exception.ConflictException;
import com.lucas.taskapi.exception.InvalidCredentialsException;
import com.lucas.taskapi.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("deve cadastrar usuário com senha criptografada e e-mail normalizado")
    void shouldRegisterUser() {
        when(userRepository.existsByEmail("lucas@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha12345")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(
                new RegisterRequest("Lucas", "  Lucas@Email.com ", "senha12345"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("hash");
        assertThat(response.email()).isEqualTo("lucas@email.com");
    }

    @Test
    @DisplayName("não deve cadastrar e-mail já existente")
    void shouldNotRegisterDuplicatedEmail() {
        when(userRepository.existsByEmail("lucas@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Lucas", "lucas@email.com", "senha12345")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("deve retornar token quando as credenciais estiverem corretas")
    void shouldLoginWithValidCredentials() {
        User user = new User("Lucas", "lucas@email.com", "hash");
        when(userRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senha12345", "hash")).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("token-jwt");
        when(tokenService.getExpirationInSeconds()).thenReturn(3600L);

        TokenResponse response = authService.login(new LoginRequest("lucas@email.com", "senha12345"));

        assertThat(response.accessToken()).isEqualTo("token-jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    @DisplayName("não deve autenticar com senha errada")
    void shouldNotLoginWithWrongPassword() {
        User user = new User("Lucas", "lucas@email.com", "hash");
        when(userRepository.findByEmail("lucas@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("lucas@email.com", "errada")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("não deve autenticar e-mail inexistente")
    void shouldNotLoginWithUnknownEmail() {
        when(userRepository.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ninguem@email.com", "senha12345")))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
