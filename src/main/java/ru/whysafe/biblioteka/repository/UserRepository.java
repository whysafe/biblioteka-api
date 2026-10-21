package ru.whysafe.biblioteka.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.whysafe.biblioteka.domain.AppUser;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByReaderId(Long readerId);
}
