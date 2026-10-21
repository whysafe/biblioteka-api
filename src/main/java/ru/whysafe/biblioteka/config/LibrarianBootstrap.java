package ru.whysafe.biblioteka.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.whysafe.biblioteka.domain.AppUser;
import ru.whysafe.biblioteka.domain.Role;
import ru.whysafe.biblioteka.repository.UserRepository;

/** Создаёт первого библиотекаря из LIBRARIAN_EMAIL / LIBRARIAN_PASSWORD, если его ещё нет. */
@Component
public class LibrarianBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LibrarianBootstrap.class);

    private final BootstrapProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public LibrarianBootstrap(BootstrapProperties props, UserRepository users, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(props.librarianEmail()) || !StringUtils.hasText(props.librarianPassword())) {
            log.info("LIBRARIAN_EMAIL не задан, первый библиотекарь не создаётся");
            return;
        }
        if (users.existsByEmailIgnoreCase(props.librarianEmail())) {
            return;
        }
        users.save(new AppUser(props.librarianEmail().toLowerCase(), encoder.encode(props.librarianPassword()),
                Role.LIBRARIAN, null));
        log.info("Создан библиотекарь {}", props.librarianEmail());
    }
}
