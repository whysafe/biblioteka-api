package ru.whysafe.biblioteka.error;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Все ошибки в формате RFC 7807 (application/problem+json).
 * Нарушения правил библиотеки получают поле code, по которому клиент может ветвиться.
 * AccessDeniedException сюда не попадает: её обрабатывает Spring Security (403).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final URI RULES = URI.create("https://github.com/whysafe/biblioteka-api#правила");

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ProblemDetail businessRule(BusinessRuleException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setType(RULES);
        problem.setTitle("Нарушено правило библиотеки");
        problem.setProperty("code", e.getCode());
        return problem;
    }

    @ExceptionHandler(AuthFailedException.class)
    ProblemDetail authFailed(AuthFailedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    /** Гонка на уникальных полях (ISBN, почта), которую не поймала проверка в сервисе. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Запись конфликтует с уже существующими данными");
        problem.setProperty("code", "CONSTRAINT_VIOLATION");
        return problem;
    }

    /** Ошибки валидации тела запроса: поле -> сообщение. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(f -> errors.putIfAbsent(f.getField(), f.getDefaultMessage()));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Проверьте поля запроса");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }
}
