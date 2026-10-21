package ru.whysafe.biblioteka.error;

/** Неверная пара почта/пароль. Отдаётся как 401 без уточнения, что именно не так. */
public class AuthFailedException extends RuntimeException {

    public AuthFailedException(String message) {
        super(message);
    }
}
