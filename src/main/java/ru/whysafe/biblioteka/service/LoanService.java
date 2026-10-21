package ru.whysafe.biblioteka.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.whysafe.biblioteka.domain.Book;
import ru.whysafe.biblioteka.domain.Loan;
import ru.whysafe.biblioteka.domain.LoanStatus;
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
import ru.whysafe.biblioteka.web.dto.IssueLoanRequest;
import ru.whysafe.biblioteka.web.dto.LoanResponse;
import ru.whysafe.biblioteka.web.dto.PageResponse;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class LoanService {

    private final LoanRepository loans;
    private final BookRepository books;
    private final ReaderRepository readers;
    private final ReservationRepository reservations;
    private final ReservationService reservationService;
    private final LoanPolicy policy;
    private final Clock clock;

    public LoanService(LoanRepository loans, BookRepository books, ReaderRepository readers,
                       ReservationRepository reservations, ReservationService reservationService,
                       LoanPolicy policy, Clock clock) {
        this.loans = loans;
        this.books = books;
        this.readers = readers;
        this.reservations = reservations;
        this.reservationService = reservationService;
        this.policy = policy;
        this.clock = clock;
    }

    /**
     * Выдача книги. Если для читателя отложен экземпляр по брони, выдаётся он;
     * иначе нужен свободный экземпляр на полке.
     */
    @Transactional
    public LoanResponse issue(IssueLoanRequest req, UserPrincipal user) {
        Long readerId = Access.targetReader(user, req.readerId());
        Reader reader = readers.findById(readerId).orElseThrow(() -> new NotFoundException("Читатель", readerId));
        Book book = books.findForUpdate(req.bookId()).orElseThrow(() -> new NotFoundException("Книга", req.bookId()));
        LocalDate today = LocalDate.now(clock);

        policy.checkCanBorrow(new LoanPolicy.BorrowerState(
                loans.countByReaderIdAndReturnedOnIsNull(readerId),
                loans.countByReaderIdAndReturnedOnIsNullAndDueDateBefore(readerId, today),
                loans.sumUnpaidFines(readerId),
                loans.existsByReaderIdAndBookIdAndReturnedOnIsNull(readerId, book.getId())));

        Optional<Reservation> own = reservations.findFirstByBookIdAndReaderIdAndStatusIn(
                book.getId(), readerId, ReservationService.OPEN);
        if (own.isPresent() && own.get().getStatus() == ReservationStatus.READY) {
            own.get().fulfil();
        } else {
            if (book.getAvailableCopies() <= 0) {
                throw new BusinessRuleException("NO_COPIES_AVAILABLE",
                        "Свободных экземпляров нет, можно встать в очередь: POST /api/reservations");
            }
            book.takeCopy();
            own.ifPresent(Reservation::fulfil);
        }
        Loan loan = loans.save(new Loan(book, reader, today, policy.dueDate(today)));
        return toResponse(loan, today);
    }

    /** Возврат: начисляет штраф за просрочку и передаёт экземпляр очереди или на полку. */
    @Transactional
    public LoanResponse returnBook(Long id) {
        Loan loan = find(id);
        if (loan.isReturned()) {
            throw new BusinessRuleException("ALREADY_RETURNED", "Книга по этой выдаче уже возвращена");
        }
        LocalDate today = LocalDate.now(clock);
        loan.markReturned(today, policy.fine(loan.getDueDate(), today));
        Book book = books.findForUpdate(loan.getBook().getId())
                .orElseThrow(() -> new NotFoundException("Книга", loan.getBook().getId()));
        reservationService.releaseCopy(book, today);
        return toResponse(loan, today);
    }

    @Transactional
    public LoanResponse renew(Long id, UserPrincipal user) {
        Loan loan = find(id);
        Access.checkOwner(user, loan.getReader().getId());
        if (loan.isReturned()) {
            throw new BusinessRuleException("ALREADY_RETURNED", "Книга по этой выдаче уже возвращена");
        }
        LocalDate today = LocalDate.now(clock);
        boolean othersWaiting = reservations.existsByBookIdAndStatus(loan.getBook().getId(), ReservationStatus.WAITING);
        policy.checkCanRenew(loan.getRenewals(), loan.getDueDate(), today, othersWaiting);
        loan.renew(policy.renewedDueDate(loan.getDueDate()));
        return toResponse(loan, today);
    }

    @Transactional
    public LoanResponse payFine(Long id) {
        Loan loan = find(id);
        if (!loan.isReturned()) {
            throw new BusinessRuleException("LOAN_OPEN", "Штраф начисляется при возврате книги");
        }
        if (loan.isFinePaid()) {
            throw new BusinessRuleException("NO_FINE", "По этой выдаче нет неоплаченного штрафа");
        }
        loan.payFine();
        return toResponse(loan, LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public LoanResponse get(Long id, UserPrincipal user) {
        Loan loan = find(id);
        Access.checkOwner(user, loan.getReader().getId());
        return toResponse(loan, LocalDate.now(clock));
    }

    /** Список выдач с фильтрами. Читатель видит только свои, параметр readerId для него игнорируется. */
    @Transactional(readOnly = true)
    public PageResponse<LoanResponse> search(Long readerId, Long bookId, LoanStatus status, UserPrincipal user,
                                             Pageable pageable) {
        Long reader = user.isLibrarian() ? readerId : Access.requireReader(user);
        LocalDate today = LocalDate.now(clock);
        Specification<Loan> spec = (root, query, cb) -> cb.conjunction();
        if (reader != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reader").get("id"), reader));
        }
        if (bookId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("book").get("id"), bookId));
        }
        if (status != null) {
            spec = spec.and(switch (status) {
                case RETURNED -> (root, query, cb) -> cb.isNotNull(root.get("returnedOn"));
                case ACTIVE -> (root, query, cb) -> cb.and(cb.isNull(root.get("returnedOn")),
                        cb.greaterThanOrEqualTo(root.get("dueDate"), today));
                case OVERDUE -> (root, query, cb) -> cb.and(cb.isNull(root.get("returnedOn")),
                        cb.lessThan(root.get("dueDate"), today));
            });
        }
        return PageResponse.of(loans.findAll(spec, pageable), loan -> toResponse(loan, today));
    }

    private Loan find(Long id) {
        return loans.findById(id).orElseThrow(() -> new NotFoundException("Выдача", id));
    }

    private LoanResponse toResponse(Loan loan, LocalDate today) {
        BigDecimal fine = loan.isReturned() ? loan.getFine() : policy.fine(loan.getDueDate(), today);
        return new LoanResponse(loan.getId(), loan.getBook().getId(), loan.getBook().getTitle(),
                loan.getReader().getId(), loan.getReader().getFullName(), loan.getIssuedOn(), loan.getDueDate(),
                loan.getReturnedOn(), loan.getRenewals(), loan.statusOn(today), fine, loan.isFinePaid());
    }
}
