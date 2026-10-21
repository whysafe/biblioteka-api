package ru.whysafe.biblioteka.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookRequest(
        @NotBlank @Pattern(regexp = "^(?:\\d{9}[\\dX]|\\d{13})$", message = "ISBN: 10 или 13 цифр без дефисов") String isbn,
        @NotBlank @Size(max = 300) String title,
        @NotBlank @Size(max = 200) String author,
        @Size(max = 100) String genre,
        @Min(1450) @Max(2100) Integer publishedYear,
        @NotNull @Min(0) @Max(1000) Integer copies) {
}
