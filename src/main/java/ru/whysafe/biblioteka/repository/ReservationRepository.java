package ru.whysafe.biblioteka.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.whysafe.biblioteka.domain.Reservation;
import ru.whysafe.biblioteka.domain.ReservationStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /** Очередь на книгу в порядке постановки. */
    List<Reservation> findByBookIdAndStatusOrderByCreatedAtAscIdAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndStatusOrderByCreatedAtAscIdAsc(Long bookId, ReservationStatus status);

    Optional<Reservation> findFirstByBookIdAndReaderIdAndStatusIn(Long bookId, Long readerId,
                                                                  Collection<ReservationStatus> statuses);

    boolean existsByBookIdAndStatus(Long bookId, ReservationStatus status);

    boolean existsByBookIdAndStatusIn(Long bookId, Collection<ReservationStatus> statuses);

    List<Reservation> findByStatusAndReadyUntilBefore(ReservationStatus status, LocalDate date);

    List<Reservation> findByReaderIdAndStatusInOrderByCreatedAtAsc(Long readerId, Collection<ReservationStatus> statuses);
}
