package ru.whysafe.biblioteka.web.dto;

import jakarta.validation.constraints.NotNull;

/** readerId обязателен для библиотекаря; читатель бронирует на себя. */
public record ReservationRequest(@NotNull Long bookId, Long readerId) {
}
