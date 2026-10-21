package ru.whysafe.biblioteka.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.whysafe.biblioteka.domain.Book;
import ru.whysafe.biblioteka.domain.ReservationStatus;
import ru.whysafe.biblioteka.error.BusinessRuleException;
import ru.whysafe.biblioteka.error.NotFoundException;
import ru.whysafe.biblioteka.repository.BookRepository;
import ru.whysafe.biblioteka.repository.LoanRepository;
import ru.whysafe.biblioteka.repository.ReservationRepository;
import ru.whysafe.biblioteka.web.dto.BookRequest;
import ru.whysafe.biblioteka.web.dto.BookResponse;
import ru.whysafe.biblioteka.web.dto.PageResponse;

import java.util.EnumSet;

@Service
public class BookService {

    private final BookRepository books;
    private final LoanRepository loans;
    private final ReservationRepository reservations;

    public BookService(BookRepository books, LoanRepository loans, ReservationRepository reservations) {
        this.books = books;
        this.loans = loans;
        this.reservations = reservations;
    }

    /** Поиск: q ищет в названии и авторе, остальные фильтры точные; любой можно опустить. */
    @Transactional(readOnly = true)
    public PageResponse<BookResponse> search(String q, String author, String genre, Boolean available,
                                             Pageable pageable) {
        Specification<Book> spec = (root, query, cb) -> cb.conjunction();
        if (StringUtils.hasText(q)) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("author")), like)));
        }
        if (StringUtils.hasText(author)) {
            String like = "%" + author.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("author")), like));
        }
        if (StringUtils.hasText(genre)) {
            String value = genre.trim().toLowerCase();
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("genre")), value));
        }
        if (Boolean.TRUE.equals(available)) {
            spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("availableCopies"), 0));
        }
        return PageResponse.of(books.findAll(spec, pageable), BookResponse::of);
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        return BookResponse.of(find(id));
    }

    @Transactional
    public BookResponse create(BookRequest req) {
        if (books.existsByIsbn(req.isbn())) {
            throw new BusinessRuleException("ISBN_TAKEN", "Книга с ISBN " + req.isbn() + " уже есть в каталоге");
        }
        Book book = new Book(req.isbn(), req.title().trim(), req.author().trim(), trimToNull(req.genre()),
                req.publishedYear(), req.copies());
        return BookResponse.of(books.save(book));
    }

    /** При изменении числа экземпляров свободные сдвигаются на ту же разницу; выданные убрать нельзя. */
    @Transactional
    public BookResponse update(Long id, BookRequest req) {
        Book book = books.findForUpdate(id).orElseThrow(() -> new NotFoundException("Книга", id));
        if (!book.getIsbn().equals(req.isbn()) && books.existsByIsbn(req.isbn())) {
            throw new BusinessRuleException("ISBN_TAKEN", "Книга с ISBN " + req.isbn() + " уже есть в каталоге");
        }
        int delta = req.copies() - book.getTotalCopies();
        int available = book.getAvailableCopies() + delta;
        if (available < 0) {
            throw new BusinessRuleException("COPIES_IN_USE",
                    "Нельзя оставить " + req.copies() + " экз.: столько выдано или отложено по брони");
        }
        book.setIsbn(req.isbn());
        book.setTitle(req.title().trim());
        book.setAuthor(req.author().trim());
        book.setGenre(trimToNull(req.genre()));
        book.setPublishedYear(req.publishedYear());
        book.setTotalCopies(req.copies());
        book.setAvailableCopies(available);
        return BookResponse.of(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = find(id);
        if (loans.existsByBookId(id)) {
            throw new BusinessRuleException("BOOK_HAS_LOANS", "У книги есть история выдач, её нельзя удалить");
        }
        if (reservations.existsByBookIdAndStatusIn(id, EnumSet.of(ReservationStatus.WAITING, ReservationStatus.READY))) {
            throw new BusinessRuleException("BOOK_HAS_RESERVATIONS", "На книгу есть активные брони");
        }
        books.delete(book);
    }

    Book find(Long id) {
        return books.findById(id).orElseThrow(() -> new NotFoundException("Книга", id));
    }

    private static String trimToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
