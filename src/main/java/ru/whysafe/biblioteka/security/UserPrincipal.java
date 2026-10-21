package ru.whysafe.biblioteka.security;

import ru.whysafe.biblioteka.domain.Role;

/** Пользователь из JWT. readerId есть только у читателя. */
public record UserPrincipal(Long userId, String email, Role role, Long readerId) {

    public boolean isLibrarian() {
        return role == Role.LIBRARIAN;
    }
}
