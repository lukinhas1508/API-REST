package com.lucas.taskapi.controller;

import com.lucas.taskapi.dto.auth.LoginRequest;
import com.lucas.taskapi.dto.auth.RegisterRequest;
import com.lucas.taskapi.dto.auth.TokenResponse;
import com.lucas.taskapi.dto.auth.UserResponse;
import com.lucas.taskapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Autenticação", description = "Cadastro e login de usuários")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Cadastra um novo usuário")
    @ApiResponse(responseCode = "201", description = "Usuário cadastrado")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Autentica o usuário e retorna um token JWT")
    @ApiResponse(responseCode = "200", description = "Login realizado")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
