package ru.whysafe.biblioteka.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 8, max = 72, message = "пароль от 8 до 72 символов") String password,
        @Pattern(regexp = "^\\+?[0-9 ()-]{5,30}$", message = "телефон: цифры, пробелы, скобки и дефисы") String phone) {
}
