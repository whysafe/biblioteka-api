package ru.whysafe.biblioteka.web.dto;

import ru.whysafe.biblioteka.domain.Role;

import java.time.Instant;

public record TokenResponse(String token, Instant expiresAt, Role role, Long readerId) {
}
