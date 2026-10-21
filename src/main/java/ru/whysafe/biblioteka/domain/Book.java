package ru.whysafe.biblioteka.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 17)
    private String isbn;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 200)
    private String author;

    @Column(length = 100)
    private String genre;

    @Column(name = "published_year")
    private Integer publishedYear;

    @Column(name = "total_copies", nullable = false)
    private int totalCopies;

    /** Экземпляры на полке: без выданных и без отложенных под бронь. */
    @Column(name = "available_copies", nullable = false)
    private int availableCopies;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Book() {
    }

    public Book(String isbn, String title, String author, String genre, Integer publishedYear, int copies) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.publishedYear = publishedYear;
        this.totalCopies = copies;
        this.availableCopies = copies;
    }

    public void takeCopy() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("Нет свободных экземпляров");
        }
        availableCopies--;
    }

    public void putCopyBack() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("Все экземпляры уже на полке");
        }
        availableCopies++;
    }

    public Long getId() {
        return id;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public void setPublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
