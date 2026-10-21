package ru.whysafe.biblioteka.service;

import org.springframework.stereotype.Component;
import ru.whysafe.biblioteka.config.LibraryProperties;
import ru.whysafe.biblioteka.error.BusinessRuleException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Правила выдачи без доступа к базе: лимит книг, сроки, продление, штрафы.
 * Сервисы собирают состояние читателя и передают сюда, поэтому правила легко проверять юнит-тестами.
 */
@Component
public class LoanPolicy {

    private final LibraryProperties props;

    public LoanPolicy(LibraryProperties props) {
        this.props = props;
    }

    /** Что известно о читателе в момент выдачи. */
    public record BorrowerState(int activeLoans, int overdueLoans, BigDecimal unpaidFines, boolean hasThisBook) {
    }

    public void checkCanBorrow(BorrowerState s) {
        if (s.hasThisBook()) {
            throw new BusinessRuleException("ALREADY_BORROWED", "Эта книга уже на руках у читателя");
        }
        if (s.overdueLoans() > 0) {
            throw new BusinessRuleException("OVERDUE_LOANS",
                    "У читателя есть просроченные книги (" + s.overdueLoans() + "), сначала их нужно вернуть");
        }
        if (s.unpaidFines().signum() > 0) {
            throw new BusinessRuleException("UNPAID_FINES",
                    "Неоплаченный штраф " + s.unpaidFines().setScale(2, RoundingMode.HALF_UP) + " руб.");
        }
        if (s.activeLoans() >= props.getMaxActiveLoans()) {
            throw new BusinessRuleException("LOAN_LIMIT_REACHED",
                    "На руках уже " + s.activeLoans() + " книг, максимум " + props.getMaxActiveLoans());
        }
    }

    public LocalDate dueDate(LocalDate issuedOn) {
        return issuedOn.plusDays(props.getLoanDays());
    }

    public long overdueDays(LocalDate dueDate, LocalDate onDate) {
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, onDate));
    }

    /** Штраф за каждый день после срока, но не больше потолка. */
    public BigDecimal fine(LocalDate dueDate, LocalDate onDate) {
        long days = overdueDays(dueDate, onDate);
        BigDecimal fine = props.getFinePerDay().multiply(BigDecimal.valueOf(days));
        return fine.min(props.getMaxFine()).setScale(2, RoundingMode.HALF_UP);
    }

    public void checkCanRenew(int renewals, LocalDate dueDate, LocalDate today, boolean othersWaiting) {
        if (today.isAfter(dueDate)) {
            throw new BusinessRuleException("LOAN_OVERDUE", "Просроченную книгу нельзя продлить, её нужно вернуть");
        }
        if (renewals >= props.getMaxRenewals()) {
            throw new BusinessRuleException("RENEWAL_LIMIT",
                    "Книгу уже продлевали " + renewals + " раз(а), больше нельзя");
        }
        if (othersWaiting) {
            throw new BusinessRuleException("RESERVED_BY_OTHERS", "На книгу есть очередь, продление недоступно");
        }
    }

    public LocalDate renewedDueDate(LocalDate dueDate) {
        return dueDate.plusDays(props.getRenewalDays());
    }

    /** До какого дня включительно отложенный экземпляр ждёт читателя. */
    public LocalDate holdUntil(LocalDate today) {
        return today.plusDays(props.getHoldDays());
    }
}
