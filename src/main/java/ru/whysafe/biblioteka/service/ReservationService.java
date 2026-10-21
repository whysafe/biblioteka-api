package ru.whysafe.biblioteka.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.whysafe.biblioteka.domain.Book;
import ru.whysafe.biblioteka.domain.Reader;
import ru.whysafe.biblioteka.domain.Reservation;
import ru.whysafe.biblioteka.domain.ReservationStatus;
import ru.whysafe.biblioteka.error.BusinessRuleException;
import ru.whysafe.biblioteka.error.NotFoundException;
import ru.whysafe.biblioteka.repository.BookRepository;
import ru.whysafe.biblioteka.repository.LoanRepository;
import ru.whysafe.biblioteka.repository.ReaderRepository;
import ru.whysafe.biblioteka.repository.ReservationRepository;
import ru.whysafe.biblioteka.security.UserPrincipal;
import ru.whysafe.biblioteka.web.dto.ReservationRequest;
import ru.whysafe.biblioteka.web.dto.ReservationResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Очередь бронирования. Бронь ставится, только когда свободных экземпляров нет.
 * Вернувшийся экземпляр не попадает на полку, а откладывается первому в очереди на holdDays дней.
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    static final Set<ReservationStatus> OPEN = EnumSet.of(ReservationStatus.WAITING, ReservationStatus.READY);

    private final ReservationRepository reservations;
    private final BookRepository books;
    private final ReaderRepository readers;
    private final LoanRepository loans;
    private final LoanPolicy policy;
    private final Clock clock;

    public ReservationService(ReservationRepository reservations, BookRepository books, ReaderRepository readers,
                              LoanRepository loans, LoanPolicy policy, Clock clock) {
        this.reservations = reservations;
        this.books = books;
        this.readers = readers;
        this.loans = loans;
        this.policy = policy;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse reserve(ReservationRequest req, UserPrincipal user) {
        Long readerId = Access.targetReader(user, req.readerId());
        Reader reader = readers.findById(readerId).orElseThrow(() -> new NotFoundException("Читатель", readerId));
        Book book = books.findForUpdate(req.bookId()).orElseThrow(() -> new NotFoundException("Книга", req.bookId()));
        if (book.getAvailableCopies() > 0) {
            throw new BusinessRuleException("COPIES_AVAILABLE", "Книга есть в наличии, её можно взять сразу");
        }
        if (loans.existsByReaderIdAndBookIdAndReturnedOnIsNull(readerId, book.getId())) {
            throw new BusinessRuleException("ALREADY_BORROWED", "Эта книга уже на руках у читателя");
        }
        if (reservations.findFirstByBookIdAndReaderIdAndStatusIn(book.getId(), readerId, OPEN).isPresent()) {
            throw new BusinessRuleException("ALREADY_RESERVED", "Читатель уже стоит в очереди на эту книгу");
        }
        Reservation saved = reservations.save(new Reservation(book, reader, Instant.now(clock)));
        return toResponse(saved);
    }

    @Transactional
    public ReservationResponse cancel(Long id, UserPrincipal user) {
        Reservation r = reservations.findById(id).orElseThrow(() -> new NotFoundException("Бронь", id));
        Access.checkOwner(user, r.getReader().getId());
        if (!r.getStatus().isOpen()) {
            throw new BusinessRuleException("RESERVATION_CLOSED", "Бронь уже закрыта: " + r.getStatus());
        }
        boolean wasHolding = r.getStatus() == ReservationStatus.READY;
        r.cancel();
        if (wasHolding) {
            releaseCopy(lock(r.getBook()), LocalDate.now(clock));
        }
        return toResponse(r);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> mine(UserPrincipal user) {
        Long readerId = Access.requireReader(user);
        return reservations.findByReaderIdAndStatusInOrderByCreatedAtAsc(readerId, OPEN).stream()
                .map(this::toResponse).toList();
    }

    /** Очередь на книгу: сначала отложенные экземпляры, затем ожидающие по порядку. */
    @Transactional(readOnly = true)
    public List<ReservationResponse> queue(Long bookId) {
        if (!books.existsById(bookId)) {
            throw new NotFoundException("Книга", bookId);
        }
        var ready = reservations.findByBookIdAndStatusOrderByCreatedAtAscIdAsc(bookId, ReservationStatus.READY);
        var waiting = reservations.findByBookIdAndStatusOrderByCreatedAtAscIdAsc(bookId, ReservationStatus.WAITING);
        return java.util.stream.Stream.concat(ready.stream(), waiting.stream()).map(this::toResponse).toList();
    }

    /**
     * Освободившийся экземпляр: отложить первому в очереди или вернуть на полку.
     * Вызывать с заблокированной книгой (BookRepository.findForUpdate).
     */
    void releaseCopy(Book book, LocalDate today) {
        reservations.findFirstByBookIdAndStatusOrderByCreatedAtAscIdAsc(book.getId(), ReservationStatus.WAITING)
                .ifPresentOrElse(next -> next.holdUntil(policy.holdUntil(today)), book::putCopyBack);
    }

    /** Снимает брони, которые не забрали вовремя, и передаёт экземпляр следующему. */
    @Transactional
    public int expireHolds() {
        LocalDate today = LocalDate.now(clock);
        List<Reservation> expired = reservations.findByStatusAndReadyUntilBefore(ReservationStatus.READY, today);
        for (Reservation r : expired) {
            r.expire();
            releaseCopy(lock(r.getBook()), today);
        }
        if (!expired.isEmpty()) {
            log.info("Снято просроченных броней: {}", expired.size());
        }
        return expired.size();
    }

    private Book lock(Book book) {
        return books.findForUpdate(book.getId()).orElseThrow(() -> new NotFoundException("Книга", book.getId()));
    }

    private ReservationResponse toResponse(Reservation r) {
        Integer position = null;
        if (r.getStatus() == ReservationStatus.WAITING) {
            List<Reservation> queue = reservations.findByBookIdAndStatusOrderByCreatedAtAscIdAsc(
                    r.getBook().getId(), ReservationStatus.WAITING);
            position = queue.stream().map(Reservation::getId).toList().indexOf(r.getId()) + 1;
        }
        return new ReservationResponse(r.getId(), r.getBook().getId(), r.getBook().getTitle(),
                r.getReader().getId(), r.getReader().getFullName(), r.getStatus(), r.getCreatedAt(),
                r.getReadyUntil(), position);
    }
}
