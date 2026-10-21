package ru.whysafe.biblioteka.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.whysafe.biblioteka.domain.AppUser;
import ru.whysafe.biblioteka.domain.Reader;
import ru.whysafe.biblioteka.domain.Role;
import ru.whysafe.biblioteka.error.AuthFailedException;
import ru.whysafe.biblioteka.error.BusinessRuleException;
import ru.whysafe.biblioteka.repository.ReaderRepository;
import ru.whysafe.biblioteka.repository.UserRepository;
import ru.whysafe.biblioteka.security.JwtService;
import ru.whysafe.biblioteka.security.UserPrincipal;
import ru.whysafe.biblioteka.web.dto.LoginRequest;
import ru.whysafe.biblioteka.web.dto.RegisterRequest;
import ru.whysafe.biblioteka.web.dto.TokenResponse;

/** Регистрация читателя и вход по почте и паролю. Библиотекари создаются только через LIBRARIAN_EMAIL. */
@Service
public class AuthService {

    private final UserRepository users;
    private final ReaderRepository readers;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final String dummyHash;

    public AuthService(UserRepository users, ReaderRepository readers, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.readers = readers;
        this.encoder = encoder;
        this.jwt = jwt;
        // Сравниваем пароль даже для несуществующей почты, чтобы по времени ответа нельзя было перебирать адреса.
        this.dummyHash = encoder.encode("timing-equalizer");
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email) || readers.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("EMAIL_TAKEN",
                    "Почта " + email + " уже используется. Если карточку завёл библиотекарь, обратитесь к нему");
        }
        Reader reader = readers.save(new Reader(req.fullName().trim(), email, ReaderService.blankToNull(req.phone())));
        AppUser user = users.save(new AppUser(email, encoder.encode(req.password()), Role.READER, reader));
        return token(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest req) {
        AppUser user = users.findByEmailIgnoreCase(req.email().trim()).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        if (!encoder.matches(req.password(), hash) || user == null) {
            throw new AuthFailedException("Неверная почта или пароль");
        }
        return token(user);
    }

    private TokenResponse token(AppUser user) {
        Long readerId = user.getReader() != null ? user.getReader().getId() : null;
        var principal = new UserPrincipal(user.getId(), user.getEmail(), user.getRole(), readerId);
        var issued = jwt.issue(principal);
        return new TokenResponse(issued.token(), issued.expiresAt(), user.getRole(), readerId);
    }
}
