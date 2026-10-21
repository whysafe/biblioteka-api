package ru.whysafe.biblioteka.web.dto;

import ru.whysafe.biblioteka.domain.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** fine: для возвращённой книги начисленный штраф, для просроченной текущий накопленный. */
public record LoanResponse(Long id, Long bookId, String bookTitle, Long readerId, String readerName,
                           LocalDate issuedOn, LocalDate dueDate, LocalDate returnedOn, int renewals,
                           LoanStatus status, BigDecimal fine, boolean finePaid) {
}
