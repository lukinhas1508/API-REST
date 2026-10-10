package com.lucas.taskapi.service;

import com.lucas.taskapi.dto.auth.LoginRequest;
import com.lucas.taskapi.dto.auth.RegisterRequest;
import com.lucas.taskapi.dto.auth.TokenResponse;
import com.lucas.taskapi.dto.auth.UserResponse;
import com.lucas.taskapi.entity.User;
import com.lucas.taskapi.exception.ConflictException;
import com.lucas.taskapi.exception.InvalidCredentialsException;
import com.lucas.taskapi.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Já existe um usuário cadastrado com este e-mail");
        }

        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        log.info("Usuário cadastrado com id {}", saved.getId());

        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        // mesma mensagem para e-mail inexistente e senha errada, para não revelar quais e-mails existem
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenService.generateToken(user);
        return TokenResponse.bearer(token, tokenService.getExpirationInSeconds());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
