package ru.whysafe.biblioteka.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.whysafe.biblioteka.domain.Reader;
import ru.whysafe.biblioteka.error.BusinessRuleException;
import ru.whysafe.biblioteka.error.NotFoundException;
import ru.whysafe.biblioteka.repository.LoanRepository;
import ru.whysafe.biblioteka.repository.ReaderRepository;
import ru.whysafe.biblioteka.web.dto.PageResponse;
import ru.whysafe.biblioteka.web.dto.ReaderRequest;
import ru.whysafe.biblioteka.web.dto.ReaderResponse;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class ReaderService {

    private final ReaderRepository readers;
    private final LoanRepository loans;
    private final Clock clock;

    public ReaderService(ReaderRepository readers, LoanRepository loans, Clock clock) {
        this.readers = readers;
        this.loans = loans;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ReaderResponse> search(String q, Pageable pageable) {
        Specification<Reader> spec = (root, query, cb) -> cb.conjunction();
        if (StringUtils.hasText(q)) {
            String like = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("email")), like)));
        }
        return PageResponse.of(readers.findAll(spec, pageable), ReaderResponse::of);
    }

    /** Карточка читателя со статистикой: сколько книг на руках, сколько просрочено, долг по штрафам. */
    @Transactional(readOnly = true)
    public ReaderResponse get(Long id) {
        Reader reader = find(id);
        LocalDate today = LocalDate.now(clock);
        return ReaderResponse.of(reader).withStats(
                loans.countByReaderIdAndReturnedOnIsNull(id),
                loans.countByReaderIdAndReturnedOnIsNullAndDueDateBefore(id, today),
                loans.sumUnpaidFines(id));
    }

    @Transactional
    public ReaderResponse create(ReaderRequest req) {
        String email = req.email().trim().toLowerCase();
        if (readers.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("EMAIL_TAKEN", "Читатель с почтой " + email + " уже есть");
        }
        return ReaderResponse.of(readers.save(new Reader(req.fullName().trim(), email, blankToNull(req.phone()))));
    }

    @Transactional
    public ReaderResponse update(Long id, ReaderRequest req) {
        Reader reader = find(id);
        String email = req.email().trim().toLowerCase();
        if (!reader.getEmail().equalsIgnoreCase(email) && readers.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("EMAIL_TAKEN", "Читатель с почтой " + email + " уже есть");
        }
        reader.setFullName(req.fullName().trim());
        reader.setEmail(email);
        reader.setPhone(blankToNull(req.phone()));
        return ReaderResponse.of(reader);
    }

    Reader find(Long id) {
        return readers.findById(id).orElseThrow(() -> new NotFoundException("Читатель", id));
    }

    static String blankToNull(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
