package ru.whysafe.biblioteka.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @Column(name = "issued_on", nullable = false)
    private LocalDate issuedOn;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "returned_on")
    private LocalDate returnedOn;

    @Column(nullable = false)
    private int renewals;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fine = BigDecimal.ZERO;

    @Column(name = "fine_paid", nullable = false)
    private boolean finePaid;

    protected Loan() {
    }

    public Loan(Book book, Reader reader, LocalDate issuedOn, LocalDate dueDate) {
        this.book = book;
        this.reader = reader;
        this.issuedOn = issuedOn;
        this.dueDate = dueDate;
    }

    public boolean isReturned() {
        return returnedOn != null;
    }

    public LoanStatus statusOn(LocalDate today) {
        if (isReturned()) {
            return LoanStatus.RETURNED;
        }
        return today.isAfter(dueDate) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE;
    }

    public void markReturned(LocalDate on, BigDecimal fine) {
        this.returnedOn = on;
        this.fine = fine;
        this.finePaid = fine.signum() == 0;
    }

    public void renew(LocalDate newDueDate) {
        this.dueDate = newDueDate;
        this.renewals++;
    }

    public void payFine() {
        this.finePaid = true;
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Reader getReader() {
        return reader;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public int getRenewals() {
        return renewals;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public boolean isFinePaid() {
        return finePaid;
    }
}
