package ru.whysafe.biblioteka.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.whysafe.biblioteka.domain.Loan;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LoanRepository extends JpaRepository<Loan, Long>, JpaSpecificationExecutor<Loan> {

    int countByReaderIdAndReturnedOnIsNull(Long readerId);

    int countByReaderIdAndReturnedOnIsNullAndDueDateBefore(Long readerId, LocalDate date);

    boolean existsByReaderIdAndBookIdAndReturnedOnIsNull(Long readerId, Long bookId);

    boolean existsByBookId(Long bookId);

    @Query("select coalesce(sum(l.fine), 0) from Loan l where l.reader.id = :readerId and l.finePaid = false")
    BigDecimal sumUnpaidFines(@Param("readerId") Long readerId);
}
