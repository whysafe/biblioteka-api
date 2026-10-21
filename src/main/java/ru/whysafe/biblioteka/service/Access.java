package ru.whysafe.biblioteka.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;
import ru.whysafe.biblioteka.security.UserPrincipal;

/** Кто над чьими данными может действовать: библиотекарь над любыми, читатель только над своими. */
final class Access {

    private Access() {
    }

    /** Для библиотекаря readerId обязателен, читатель всегда действует от своего имени. */
    static Long targetReader(UserPrincipal user, Long requestedReaderId) {
        if (user.isLibrarian()) {
            if (requestedReaderId == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите readerId");
            }
            return requestedReaderId;
        }
        if (requestedReaderId != null && !requestedReaderId.equals(user.readerId())) {
            throw new AccessDeniedException("Читатель может действовать только от своего имени");
        }
        return requireReader(user);
    }

    static void checkOwner(UserPrincipal user, Long ownerReaderId) {
        if (!user.isLibrarian() && !ownerReaderId.equals(user.readerId())) {
            throw new AccessDeniedException("Это чужая запись");
        }
    }

    static Long requireReader(UserPrincipal user) {
        if (user.readerId() == null) {
            throw new AccessDeniedException("У учётной записи нет читательского билета");
        }
        return user.readerId();
    }
}
