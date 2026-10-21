package ru.whysafe.biblioteka.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reader_id", nullable = false)
    private Reader reader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.WAITING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "ready_until")
    private LocalDate readyUntil;

    protected Reservation() {
    }

    public Reservation(Book book, Reader reader, Instant createdAt) {
        this.book = book;
        this.reader = reader;
        this.createdAt = createdAt;
    }

    public void holdUntil(LocalDate date) {
        this.status = ReservationStatus.READY;
        this.readyUntil = date;
    }

    public void fulfil() {
        this.status = ReservationStatus.FULFILLED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public void expire() {
        this.status = ReservationStatus.EXPIRED;
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

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public LocalDate getReadyUntil() {
        return readyUntil;
    }
}
