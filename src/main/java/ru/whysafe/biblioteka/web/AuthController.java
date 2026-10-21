package ru.whysafe.biblioteka.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.whysafe.biblioteka.service.AuthService;
import ru.whysafe.biblioteka.web.dto.LoginRequest;
import ru.whysafe.biblioteka.web.dto.RegisterRequest;
import ru.whysafe.biblioteka.web.dto.TokenResponse;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Вход", description = "Регистрация читателя и получение JWT")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Зарегистрироваться читателем", description = "Создаёт карточку читателя и сразу выдаёт токен")
    public TokenResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @PostMapping("/login")
    @Operation(summary = "Войти и получить токен")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }
}
