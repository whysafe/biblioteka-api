package ru.whysafe.biblioteka.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.whysafe.biblioteka.security.UserPrincipal;
import ru.whysafe.biblioteka.service.ReaderService;
import ru.whysafe.biblioteka.web.dto.PageResponse;
import ru.whysafe.biblioteka.web.dto.ReaderRequest;
import ru.whysafe.biblioteka.web.dto.ReaderResponse;

@RestController
@RequestMapping("/api/readers")
@Tag(name = "Читатели")
public class ReaderController {

    private final ReaderService readers;

    public ReaderController(ReaderService readers) {
        this.readers = readers;
    }

    @GetMapping
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Поиск читателей", description = "q ищет в имени и почте")
    public PageResponse<ReaderResponse> search(@RequestParam(required = false) String q,
                                               @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC)
                                               Pageable pageable) {
        return readers.search(q, pageable);
    }

    @GetMapping("/me")
    @Operation(summary = "Моя карточка", description = "Сколько книг на руках, просрочки и долг по штрафам")
    public ReaderResponse me(@AuthenticationPrincipal UserPrincipal user) {
        if (user.readerId() == null) {
            throw new AccessDeniedException("У учётной записи нет читательского билета");
        }
        return readers.get(user.readerId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Карточка читателя со статистикой")
    public ReaderResponse get(@PathVariable Long id) {
        return readers.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Завести карточку читателя", description = "Без учётной записи: для тех, кто приходит лично")
    public ReaderResponse create(@Valid @RequestBody ReaderRequest req) {
        return readers.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Изменить карточку читателя")
    public ReaderResponse update(@PathVariable Long id, @Valid @RequestBody ReaderRequest req) {
        return readers.update(id, req);
    }
}
