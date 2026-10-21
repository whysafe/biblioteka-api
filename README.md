# Библиотека API: выдачи, штрафы и очередь на книги

REST API для небольшой библиотеки на Spring Boot 3 и PostgreSQL. Библиотекарь ведёт каталог и карточки читателей, выдаёт и принимает книги. Читатели регистрируются сами, берут книги, продлевают их и встают в очередь, если свободных экземпляров нет. Штрафы за просрочку считаются автоматически, а вернувшийся экземпляр откладывается первому в очереди.

![Java](https://img.shields.io/badge/Java_21-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=flat&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-4169E1?style=flat&logo=postgresql&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat&logo=flyway&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=flat&logo=jsonwebtokens&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)
![Testcontainers](https://img.shields.io/badge/Testcontainers-291A3F?style=flat&logo=docker&logoColor=white)

## Возможности

**Каталог**
- Поиск по названию и автору, фильтры по жанру и наличию, пагинация и сортировка. Каталог открыт без входа.
- Книга хранит общее число экземпляров и сколько сейчас на полке. Уменьшить тираж ниже выданного нельзя, удалить книгу с историей выдач тоже.

<a id="правила"></a>
**Правила выдачи** (настраиваются в `application.yml`)
- Не больше 5 книг на руках, срок 14 дней.
- Не выдаём, если есть просроченные книги или неоплаченный штраф.
- Продление на 14 дней, не больше двух раз, и только если на книгу нет очереди и срок ещё не прошёл.
- Штраф 10 ₽ за день просрочки, не больше 500 ₽. Начисляется при возврате и оплачивается отметкой библиотекаря.

**Очередь бронирования**
- Встать в очередь можно, только когда свободных экземпляров нет.
- При возврате экземпляр не возвращается на полку, а откладывается первому в очереди на 3 дня. Он виден в `/api/reservations/mine` со сроком `readyUntil`.
- Если не забрали вовремя, ночная задача снимает бронь и передаёт экземпляр следующему. Отменённая бронь с отложенным экземпляром делает то же самое сразу.
- Выдача и возврат блокируют строку книги (`SELECT … FOR UPDATE`), чтобы два запроса не взяли один последний экземпляр.

**Доступ**
- JWT в заголовке `Authorization: Bearer …`, роли `LIBRARIAN` и `READER`.
- Читатель видит и продлевает только свои выдачи и брони, библиотекарь работает с любыми.
- Первый библиотекарь создаётся при старте из `LIBRARIAN_EMAIL` / `LIBRARIAN_PASSWORD`.
- Пароли хранятся в BCrypt. При входе пароль сравнивается даже для несуществующей почты, чтобы по времени ответа нельзя было перебирать адреса.

**Ошибки** в формате RFC 7807 (`application/problem+json`). Нарушение правила отдаётся как 409 с машиночитаемым `code`, ошибки полей приходят списком:

```json
{
  "type": "https://github.com/whysafe/biblioteka-api#правила",
  "title": "Нарушено правило библиотеки",
  "status": 409,
  "detail": "Неоплаченный штраф 60.00 руб.",
  "code": "UNPAID_FINES"
}
```

## Эндпоинты

Полная документация с примерами в Swagger UI: `http://localhost:8080/swagger-ui.html`.

| Метод и путь | Кто | Что делает |
|---|---|---|
| `POST /api/auth/register` | все | регистрация читателя, сразу возвращает токен |
| `POST /api/auth/login` | все | вход, возвращает токен |
| `GET /api/books`, `GET /api/books/{id}` | все | каталог |
| `POST`, `PUT /api/books/{id}`, `DELETE /api/books/{id}` | библиотекарь | управление каталогом |
| `GET /api/books/{id}/queue` | библиотекарь | очередь на книгу |
| `GET /api/readers/me` | читатель | своя карточка: книги на руках, просрочки, долг |
| `GET`, `POST`, `PUT /api/readers…` | библиотекарь | карточки читателей |
| `POST /api/loans` | оба | выдать книгу (библиотекарь указывает `readerId`) |
| `GET /api/loans`, `GET /api/loans/{id}` | оба | выдачи, фильтр `status=ACTIVE/OVERDUE/RETURNED` |
| `POST /api/loans/{id}/renew` | оба | продлить |
| `POST /api/loans/{id}/return` | библиотекарь | принять книгу, начислить штраф |
| `POST /api/loans/{id}/pay-fine` | библиотекарь | отметить штраф оплаченным |
| `POST /api/reservations`, `DELETE /api/reservations/{id}` | оба | встать в очередь, отменить |
| `GET /api/reservations/mine` | читатель | свои брони с местом в очереди |

## Запуск

Через Docker Compose (API и PostgreSQL):

```bash
cp .env.example .env      # заполните DB_PASSWORD, JWT_SECRET и данные библиотекаря
docker compose up --build
```

Локально, если PostgreSQL уже есть:

```bash
export DB_PASSWORD=... JWT_SECRET=$(openssl rand -base64 48)
export LIBRARIAN_EMAIL=librarian@example.com LIBRARIAN_PASSWORD=...
./mvnw spring-boot:run
```

Пример сценария:

```bash
TOKEN=$(curl -s localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"fullName":"Анна Петрова","email":"anna@example.com","password":"password123"}' | jq -r .token)

curl -s 'localhost:8080/api/books?q=мастер'
curl -s localhost:8080/api/loans -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"bookId":1}'
```

При первом запуске миграция добавляет в каталог несколько книг для примера.

## Как устроено

```
domain/        сущности JPA: Book, Reader, Loan, Reservation, AppUser
repository/    Spring Data JPA, поиск через Specification, блокировка книги при выдаче
service/       LoanPolicy (правила без БД), LoanService, ReservationService, AuthService, ночная задача
security/      JWT-фильтр, конфигурация доступа, обработчики 401/403 в формате problem+json
web/           контроллеры и DTO-записи с Bean Validation
error/         исключения и @RestControllerAdvice
db/migration/  схема Flyway с CHECK-ограничениями и частичными индексами
```

- **Правила отделены от базы.** `LoanPolicy` получает состояние читателя (сколько книг, просрочки, долг) и решает, можно ли выдать или продлить. Поэтому все правила проверяются обычными юнит-тестами без Spring.
- **Время берётся из бина `Clock`.** Выдачи, сроки и штрафы считаются от него, и в тестах часы переводятся на недели вперёд.
- **Права проверяются дважды:** по URL в фильтре безопасности (чтобы читатель получил 403 до валидации тела) и `@PreAuthorize` на методах контроллеров.
- **Целостность на уровне схемы:** `available_copies <= total_copies`, перечисления статусов через `CHECK`, частичные индексы по активным выдачам.
- `spring.jpa.hibernate.ddl-auto=validate`: схемой управляет только Flyway, а Hibernate проверяет, что сущности ей соответствуют.

## Тесты

```bash
./mvnw verify        # нужен Docker для Testcontainers
```

- **Юнит-тесты `LoanPolicy`:** лимит книг, просрочки, долг, расчёт и потолок штрафа, правила продления.
- **Интеграционный тест `LibraryFlowIT`** на PostgreSQL 16 в Testcontainers проходит весь путь через HTTP:
  - выдача последнего экземпляра и отказ второму читателю с кодом `NO_COPIES_AVAILABLE`, затем очередь;
  - запрет продления при очереди;
  - возврат через 20 дней со штрафом 60 ₽, отказ в новой книге до оплаты;
  - экземпляр, отложенный следующему, и его выдача;
  - истечение брони и возврат экземпляра на полку;
  - чужие выдачи недоступны, читатель не может менять каталог;
  - 401, 403 и ошибки валидации в формате problem+json.

В GitHub Actions на каждый push запускается `./mvnw verify` на JDK 21 и проверяется сборка Docker-образа.

## Автор

whysafe · [whysafe.ru](https://whysafe.ru) · Telegram [@yawhysafe](https://t.me/yawhysafe) · канал [@whysafe_official](https://t.me/whysafe_official) · [Kwork](https://kwork.ru/user/whysafe) · hello@whysafe.ru

## Лицензия

MIT
