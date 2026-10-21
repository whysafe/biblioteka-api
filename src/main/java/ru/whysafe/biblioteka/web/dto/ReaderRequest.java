package ru.whysafe.biblioteka.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReaderRequest(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Email @Size(max = 200) String email,
        @Pattern(regexp = "^\\+?[0-9 ()-]{5,30}$", message = "телефон: цифры, пробелы, скобки и дефисы") String phone) {
}
