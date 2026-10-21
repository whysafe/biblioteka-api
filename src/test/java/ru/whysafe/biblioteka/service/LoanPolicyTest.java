package ru.whysafe.biblioteka.service;

import org.junit.jupiter.api.Test;
import ru.whysafe.biblioteka.config.LibraryProperties;
import ru.whysafe.biblioteka.error.BusinessRuleException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class LoanPolicyTest {

    private final LoanPolicy policy = new LoanPolicy(defaults());
    private final LocalDate due = LocalDate.of(2026, 10, 15);

    private static LibraryProperties defaults() {
        LibraryProperties p = new LibraryProperties();
        p.setMaxActiveLoans(5);
        p.setLoanDays(14);
        p.setRenewalDays(14);
        p.setMaxRenewals(2);
        p.setFinePerDay(new BigDecimal("10.00"));
        p.setMaxFine(new BigDecimal("500.00"));
        p.setHoldDays(3);
        return p;
    }

    private static LoanPolicy.BorrowerState state(int active, int overdue, String fines, boolean hasBook) {
        return new LoanPolicy.BorrowerState(active, overdue, new BigDecimal(fines), hasBook);
    }

    private static String codeOf(Runnable action) {
        try {
            action.run();
            return null;
        } catch (BusinessRuleException e) {
            return e.getCode();
        }
    }

    @Test
    void clean_reader_under_the_limit_can_borrow() {
        assertThatNoException().isThrownBy(() -> policy.checkCanBorrow(state(4, 0, "0", false)));
    }

    @Test
    void borrowing_is_refused_with_a_specific_code() {
        assertThat(codeOf(() -> policy.checkCanBorrow(state(5, 0, "0", false)))).isEqualTo("LOAN_LIMIT_REACHED");
        assertThat(codeOf(() -> policy.checkCanBorrow(state(1, 1, "0", false)))).isEqualTo("OVERDUE_LOANS");
        assertThat(codeOf(() -> policy.checkCanBorrow(state(0, 0, "0.01", false)))).isEqualTo("UNPAID_FINES");
        assertThat(codeOf(() -> policy.checkCanBorrow(state(0, 0, "0", true)))).isEqualTo("ALREADY_BORROWED");
    }

    @Test
    void due_date_is_two_weeks_after_issue() {
        assertThat(policy.dueDate(LocalDate.of(2026, 10, 1))).isEqualTo(LocalDate.of(2026, 10, 15));
    }

    @Test
    void no_fine_on_or_before_the_due_date() {
        assertThat(policy.fine(due, due)).isEqualByComparingTo("0");
        assertThat(policy.fine(due, due.minusDays(3))).isEqualByComparingTo("0");
    }

    @Test
    void fine_grows_per_day_and_is_capped() {
        assertThat(policy.fine(due, due.plusDays(1))).isEqualByComparingTo("10.00");
        assertThat(policy.fine(due, due.plusDays(7))).isEqualByComparingTo("70.00");
        assertThat(policy.fine(due, due.plusDays(365))).isEqualByComparingTo("500.00");
        assertThat(policy.fine(due, due.plusDays(7)).scale()).isEqualTo(2);
    }

    @Test
    void renewal_rules() {
        assertThatNoException().isThrownBy(() -> policy.checkCanRenew(0, due, due, false));
        assertThat(codeOf(() -> policy.checkCanRenew(0, due, due.plusDays(1), false))).isEqualTo("LOAN_OVERDUE");
        assertThat(codeOf(() -> policy.checkCanRenew(2, due, due, false))).isEqualTo("RENEWAL_LIMIT");
        assertThat(codeOf(() -> policy.checkCanRenew(0, due, due, true))).isEqualTo("RESERVED_BY_OTHERS");
        assertThat(policy.renewedDueDate(due)).isEqualTo(due.plusDays(14));
    }

    @Test
    void reserved_copy_waits_three_days() {
        assertThat(policy.holdUntil(due)).isEqualTo(due.plusDays(3));
    }

    @Test
    void error_message_explains_the_limit() {
        assertThatThrownBy(() -> policy.checkCanBorrow(state(5, 0, "0", false)))
                .hasMessageContaining("максимум 5");
    }
}
