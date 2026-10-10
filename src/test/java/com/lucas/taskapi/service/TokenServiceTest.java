package com.lucas.taskapi.service;

import com.lucas.taskapi.config.JwtProperties;
import com.lucas.taskapi.config.SecurityConfig;
import com.lucas.taskapi.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Usa o encoder e o decoder reais (sem mocks) para garantir que o token
 * gerado é aceito pela mesma configuração que valida as requisições.
 */
class TokenServiceTest {

    private static final String SECRET = "segredo-usado-apenas-nos-testes-automatizados-0123456789";

    private TokenService tokenService;
    private JwtDecoder jwtDecoder;
    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET, "task-api", Duration.ofHours(1));
        SecurityConfig securityConfig = new SecurityConfig(properties);

        tokenService = new TokenService(securityConfig.jwtEncoder(), properties);
        jwtDecoder = securityConfig.jwtDecoder();

        user = new User("Lucas", "lucas@email.com", "hash");
        ReflectionTestUtils.setField(user, "id", 42L);
    }

    @Test
    @DisplayName("deve gerar token com id do usuário, e-mail e emissor")
    void shouldGenerateValidToken() {
        String token = tokenService.generateToken(user);

        Jwt jwt = jwtDecoder.decode(token);

        assertThat(jwt.getSubject()).isEqualTo("42");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("lucas@email.com");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("task-api");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    @DisplayName("deve recusar token assinado com outro segredo")
    void shouldRejectTokenSignedWithAnotherSecret() {
        JwtProperties otherProperties = new JwtProperties(
                "outro-segredo-qualquer-com-mais-de-32-caracteres-9876543210", "task-api", Duration.ofHours(1));
        TokenService otherTokenService = new TokenService(new SecurityConfig(otherProperties).jwtEncoder(), otherProperties);

        String foreignToken = otherTokenService.generateToken(user);

        assertThatThrownBy(() -> jwtDecoder.decode(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("deve informar a expiração em segundos")
    void shouldReturnExpirationInSeconds() {
        assertThat(tokenService.getExpirationInSeconds()).isEqualTo(3600);
    }
}
