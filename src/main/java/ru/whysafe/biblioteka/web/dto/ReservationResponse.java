package ru.whysafe.biblioteka.web.dto;

import ru.whysafe.biblioteka.domain.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;

/** queuePosition: место в очереди для WAITING, для остальных статусов не выводится. */
public record ReservationResponse(Long id, Long bookId, String bookTitle, Long readerId, String readerName,
                                  ReservationStatus status, Instant createdAt, LocalDate readyUntil,
                                  Integer queuePosition) {
}
