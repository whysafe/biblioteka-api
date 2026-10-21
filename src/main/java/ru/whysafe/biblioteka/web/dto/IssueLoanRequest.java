package ru.whysafe.biblioteka.web.dto;

import jakarta.validation.constraints.NotNull;

/** readerId обязателен для библиотекаря; читатель всегда берёт книгу на себя. */
public record IssueLoanRequest(@NotNull Long bookId, Long readerId) {
}
