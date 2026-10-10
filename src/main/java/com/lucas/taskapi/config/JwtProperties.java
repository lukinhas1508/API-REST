package com.lucas.taskapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configurações do token JWT, lidas do application.yml (prefixo security.jwt).
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration expiration
) {
}
