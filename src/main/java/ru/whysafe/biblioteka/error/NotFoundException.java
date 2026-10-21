package ru.whysafe.biblioteka.error;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String entity, Object id) {
        super(entity + " с id=" + id + " не найден(а)");
    }
}
