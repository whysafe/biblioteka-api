package ru.whysafe.biblioteka;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.whysafe.biblioteka.service.ReservationService;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Сквозной сценарий на настоящем PostgreSQL: выдача, очередь, продление, просрочка и штраф, отложенный
 * экземпляр, права ролей и формат ошибок. Время двигается подменёнными часами.
 */
@SpringBootTest(properties = {
        "app.jwt.secret=test-secret-test-secret-test-secret-123",
        // Тест переводит часы на недели вперёд, токен не должен успеть истечь.
        "app.jwt.ttl=P365D",
        "app.bootstrap.librarian-email=librarian@test.local",
        "app.bootstrap.librarian-password=librarian-pass",
})
@AutoConfigureMockMvc
@Testcontainers
class LibraryFlowIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static final Instant START = Instant.parse("2026-10-01T09:00:00Z");

    @TestConfiguration
    static class Clocks {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(START);
        }
    }

    /** Часы, которые тест может переводить вперёд. */
    static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        void set(Instant instant) {
            now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    MutableClock clock;

    @Autowired
    ReservationService reservations;

    String librarian;

    @BeforeEach
    void login() throws Exception {
        clock.set(START);
        librarian = token(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"librarian@test.local\",\"password\":\"librarian-pass\"}"));
    }

    // ---------- helpers ----------

    private String token(MockHttpServletRequestBuilder req) throws Exception {
        String body = mvc.perform(req).andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private String newReader(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        return token(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"password123\"}"));
    }

    private ResultActions call(MockHttpServletRequestBuilder req, String token, String json) throws Exception {
        if (token != null) {
            req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (json != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(req);
    }

    private long id(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private long newBook(int copies) throws Exception {
        String isbn = String.valueOf(9_780_000_000_000L + (long) (Math.random() * 999_999_999));
        return id(call(post("/api/books"), librarian, """
                {"isbn":"%s","title":"Тестовая книга %s","author":"Автор","copies":%d}
                """.formatted(isbn, isbn, copies)).andExpect(status().isCreated()));
    }

    private ResultActions borrow(String reader, long bookId) throws Exception {
        return call(post("/api/loans"), reader, "{\"bookId\":" + bookId + "}");
    }

    // ---------- scenarios ----------

    @Test
    void catalog_is_public_and_seeded_with_demo_books() throws Exception {
        call(get("/api/books").param("q", "мастер"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Мастер и Маргарита"))
                .andExpect(jsonPath("$.content[0].availableCopies").value(4));
    }

    @Test
    void queue_renewal_fine_and_hold_work_together() throws Exception {
        long book = newBook(1);
        String anna = newReader("Анна");
        String boris = newReader("Борис");

        long annaLoan = id(borrow(anna, book).andExpect(status().isCreated())
                .andExpect(jsonPath("$.dueDate").value("2026-10-15")));

        // Экземпляр один: Борис не может взять, но встаёт в очередь.
        borrow(boris, book).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("NO_COPIES_AVAILABLE"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        call(post("/api/reservations"), boris, "{\"bookId\":" + book + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.queuePosition").value(1));

        // Пока есть очередь, продлить нельзя.
        call(post("/api/loans/" + annaLoan + "/renew"), anna, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVED_BY_OTHERS"));

        // Возврат через 20 дней: 6 дней просрочки по 10 руб.
        clock.advance(Duration.ofDays(20));
        call(post("/api/loans/" + annaLoan + "/return"), librarian, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.fine").value(60.0))
                .andExpect(jsonPath("$.finePaid").value(false));

        // С долгом новую книгу не дают, после оплаты дают.
        long other = newBook(2);
        borrow(anna, other).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("UNPAID_FINES"));
        call(post("/api/loans/" + annaLoan + "/pay-fine"), librarian, null).andExpect(jsonPath("$.finePaid").value(true));
        borrow(anna, other).andExpect(status().isCreated());

        // Вернувшийся экземпляр отложен Борису: на полке его нет, а Борис его получает.
        call(get("/api/books/" + book), null, null).andExpect(jsonPath("$.availableCopies").value(0));
        call(get("/api/reservations/mine"), boris, null)
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[0].readyUntil").value("2026-10-24"));
        borrow(boris, book).andExpect(status().isCreated());
        call(get("/api/reservations/mine"), boris, null).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void uncollected_hold_expires_and_goes_back_to_the_shelf() throws Exception {
        long book = newBook(1);
        String owner = newReader("Вера");
        String waiter = newReader("Глеб");
        long loan = id(borrow(owner, book));
        call(post("/api/reservations"), waiter, "{\"bookId\":" + book + "}").andExpect(status().isCreated());
        call(post("/api/loans/" + loan + "/return"), librarian, null).andExpect(status().isOk());

        clock.advance(Duration.ofDays(4));
        reservations.expireHolds();

        call(get("/api/reservations/mine"), waiter, null).andExpect(jsonPath("$", hasSize(0)));
        call(get("/api/books/" + book), null, null).andExpect(jsonPath("$.availableCopies").value(1));
    }

    @Test
    void readers_see_only_their_own_loans_and_cannot_manage_the_catalog() throws Exception {
        long book = newBook(3);
        String dina = newReader("Дина");
        String egor = newReader("Егор");
        long loan = id(borrow(dina, book));

        call(get("/api/loans"), egor, null).andExpect(jsonPath("$.totalElements").value(0));
        call(get("/api/loans/" + loan), egor, null).andExpect(status().isForbidden());
        call(get("/api/loans/" + loan), dina, null).andExpect(status().isOk());
        call(post("/api/books"), dina, "{}").andExpect(status().isForbidden());
        call(post("/api/loans/" + loan + "/return"), dina, null).andExpect(status().isForbidden());
        call(get("/api/readers/me"), dina, null).andExpect(jsonPath("$.activeLoans").value(1));
        call(delete("/api/books/" + book), librarian, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_HAS_LOANS"));
    }

    @Test
    void auth_and_validation_errors_are_problem_details() throws Exception {
        call(get("/api/loans"), null, null)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        call(get("/api/loans"), "not-a-jwt", null).andExpect(status().isUnauthorized());
        call(post("/api/auth/login"), null, "{\"email\":\"librarian@test.local\",\"password\":\"wrong\"}")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Неверная почта или пароль"));
        call(post("/api/auth/register"), null, "{\"fullName\":\"\",\"email\":\"nope\",\"password\":\"short\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").value("пароль от 8 до 72 символов"));
        call(post("/api/books"), librarian, """
                {"isbn":"123","title":"x","author":"y","copies":1}
                """).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.isbn").exists());
    }
}
