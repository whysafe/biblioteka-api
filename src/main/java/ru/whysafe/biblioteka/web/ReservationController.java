package ru.whysafe.biblioteka.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.whysafe.biblioteka.security.UserPrincipal;
import ru.whysafe.biblioteka.service.ReservationService;
import ru.whysafe.biblioteka.web.dto.ReservationRequest;
import ru.whysafe.biblioteka.web.dto.ReservationResponse;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Брони", description = "Очередь на книги, которых сейчас нет на полке")
public class ReservationController {

    private final ReservationService reservations;

    public ReservationController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Встать в очередь", description = "Только если свободных экземпляров нет")
    public ReservationResponse reserve(@Valid @RequestBody ReservationRequest req,
                                       @AuthenticationPrincipal UserPrincipal user) {
        return reservations.reserve(req, user);
    }

    @GetMapping("/mine")
    @Operation(summary = "Мои брони", description = "С местом в очереди или сроком, до которого книга отложена")
    public List<ReservationResponse> mine(@AuthenticationPrincipal UserPrincipal user) {
        return reservations.mine(user);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Отменить бронь", description = "Если экземпляр уже был отложен, он уходит следующему в очереди")
    public ReservationResponse cancel(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal user) {
        return reservations.cancel(id, user);
    }
}
