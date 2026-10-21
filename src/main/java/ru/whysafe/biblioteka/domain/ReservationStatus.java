package ru.whysafe.biblioteka.domain;

public enum ReservationStatus {
    /** Читатель стоит в очереди. */
    WAITING,
    /** Экземпляр отложен для читателя до readyUntil. */
    READY,
    /** Книга выдана по брони. */
    FULFILLED,
    CANCELLED,
    /** Читатель не забрал отложенную книгу вовремя. */
    EXPIRED;

    public boolean isOpen() {
        return this == WAITING || this == READY;
    }
}
