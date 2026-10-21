package ru.whysafe.biblioteka.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.whysafe.biblioteka.service.BookService;
import ru.whysafe.biblioteka.service.ReservationService;
import ru.whysafe.biblioteka.web.dto.BookRequest;
import ru.whysafe.biblioteka.web.dto.BookResponse;
import ru.whysafe.biblioteka.web.dto.PageResponse;
import ru.whysafe.biblioteka.web.dto.ReservationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@Tag(name = "Книги", description = "Каталог открыт всем, изменения только библиотекарю")
public class BookController {

    private final BookService books;
    private final ReservationService reservations;

    public BookController(BookService books, ReservationService reservations) {
        this.books = books;
        this.reservations = reservations;
    }

    @GetMapping
    @Operation(summary = "Поиск по каталогу",
            description = "q ищет в названии и авторе; сортировка: sort=title,asc / author / publishedYear")
    public PageResponse<BookResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) String author,
                                             @RequestParam(required = false) String genre,
                                             @RequestParam(required = false) Boolean available,
                                             @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC)
                                             Pageable pageable) {
        return books.search(q, author, genre, available, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Карточка книги")
    public BookResponse get(@PathVariable Long id) {
        return books.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Добавить книгу")
    public BookResponse create(@Valid @RequestBody BookRequest req) {
        return books.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Изменить книгу", description = "Число экземпляров нельзя сделать меньше выданных")
    public BookResponse update(@PathVariable Long id, @Valid @RequestBody BookRequest req) {
        return books.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Удалить книгу", description = "Только если по ней не было выдач и нет активных броней")
    public void delete(@PathVariable Long id) {
        books.delete(id);
    }

    @GetMapping("/{id}/queue")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Очередь бронирования на книгу")
    public List<ReservationResponse> queue(@PathVariable Long id) {
        return reservations.queue(id);
    }
}
