package ru.whysafe.biblioteka.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import ru.whysafe.biblioteka.domain.Reader;

import java.util.Optional;

public interface ReaderRepository extends JpaRepository<Reader, Long>, JpaSpecificationExecutor<Reader> {

    Optional<Reader> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
