package ru.whysafe.biblioteka.web.dto;

import ru.whysafe.biblioteka.domain.Book;

public record BookResponse(Long id, String isbn, String title, String author, String genre, Integer publishedYear,
                           int totalCopies, int availableCopies) {

    public static BookResponse of(Book b) {
        return new BookResponse(b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getGenre(),
                b.getPublishedYear(), b.getTotalCopies(), b.getAvailableCopies());
    }
}
