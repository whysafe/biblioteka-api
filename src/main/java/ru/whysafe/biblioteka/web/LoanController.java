package ru.whysafe.biblioteka.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.whysafe.biblioteka.domain.LoanStatus;
import ru.whysafe.biblioteka.security.UserPrincipal;
import ru.whysafe.biblioteka.service.LoanService;
import ru.whysafe.biblioteka.web.dto.IssueLoanRequest;
import ru.whysafe.biblioteka.web.dto.LoanResponse;
import ru.whysafe.biblioteka.web.dto.PageResponse;

@RestController
@RequestMapping("/api/loans")
@Tag(name = "Выдачи", description = "Выдача, возврат, продление и штрафы")
public class LoanController {

    private final LoanService loans;

    public LoanController(LoanService loans) {
        this.loans = loans;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Выдать книгу",
            description = "Библиотекарь указывает readerId. Если книга отложена читателю по брони, выдаётся отложенный экземпляр")
    public LoanResponse issue(@Valid @RequestBody IssueLoanRequest req, @AuthenticationPrincipal UserPrincipal user) {
        return loans.issue(req, user);
    }

    @GetMapping
    @Operation(summary = "Список выдач", description = "Читатель видит только свои. status: ACTIVE, OVERDUE, RETURNED")
    public PageResponse<LoanResponse> search(@RequestParam(required = false) Long readerId,
                                             @RequestParam(required = false) Long bookId,
                                             @RequestParam(required = false) LoanStatus status,
                                             @AuthenticationPrincipal UserPrincipal user,
                                             @PageableDefault(size = 20, sort = "issuedOn", direction = Sort.Direction.DESC)
                                             Pageable pageable) {
        return loans.search(readerId, bookId, status, user, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Выдача")
    public LoanResponse get(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return loans.get(id, user);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Принять книгу", description = "Начисляет штраф за просрочку и отдаёт экземпляр первому в очереди")
    public LoanResponse returnBook(@PathVariable Long id) {
        return loans.returnBook(id);
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Продлить", description = "Нельзя, если срок уже прошёл, лимит продлений исчерпан или на книгу очередь")
    public LoanResponse renew(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return loans.renew(id, user);
    }

    @PostMapping("/{id}/pay-fine")
    @PreAuthorize("hasRole('LIBRARIAN')")
    @Operation(summary = "Отметить штраф оплаченным")
    public LoanResponse payFine(@PathVariable Long id) {
        return loans.payFine(id);
    }
}
