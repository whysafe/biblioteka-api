package ru.whysafe.biblioteka.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;
import ru.whysafe.biblioteka.config.JwtProperties;
import ru.whysafe.biblioteka.domain.Role;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final JwtProperties props;
    private final Clock clock;

    public JwtService(JwtProperties props, Clock clock) {
        if (props.secret() == null || props.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET должен быть задан и содержать не меньше 32 байт");
        }
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
        this.props = props;
        this.clock = clock;
    }

    public IssuedToken issue(UserPrincipal user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(props.ttl());
        var builder = Jwts.builder()
                .subject(String.valueOf(user.userId()))
                .claim("email", user.email())
                .claim("role", user.role().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt));
        if (user.readerId() != null) {
            builder.claim("readerId", user.readerId());
        }
        return new IssuedToken(builder.signWith(key).compact(), expiresAt);
    }

    public Optional<UserPrincipal> parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number readerId = claims.get("readerId", Number.class);
            return Optional.of(new UserPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    readerId == null ? null : readerId.longValue()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }
}
