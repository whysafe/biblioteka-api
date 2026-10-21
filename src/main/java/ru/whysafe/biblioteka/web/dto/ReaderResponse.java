package ru.whysafe.biblioteka.web.dto;

import ru.whysafe.biblioteka.domain.Reader;

import java.math.BigDecimal;
import java.time.Instant;

/** Статистика (activeLoans, overdueLoans, unpaidFines) заполняется только в карточке читателя, не в списке. */
public record ReaderResponse(Long id, String fullName, String email, String phone, Instant createdAt,
                             Integer activeLoans, Integer overdueLoans, BigDecimal unpaidFines) {

    public static ReaderResponse of(Reader r) {
        return new ReaderResponse(r.getId(), r.getFullName(), r.getEmail(), r.getPhone(), r.getCreatedAt(),
                null, null, null);
    }

    public ReaderResponse withStats(int active, int overdue, BigDecimal unpaid) {
        return new ReaderResponse(id, fullName, email, phone, createdAt, active, overdue, unpaid);
    }
}
