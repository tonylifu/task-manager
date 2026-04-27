# Task Manager API — Spring Boot 3.5.x

Enterprise-grade REST API for task management. Built with Spring Boot 3.5.x, Java 21, PostgreSQL, Flyway, MapStruct, and a comprehensive test suite.

> **Note on Spring Boot 4:** Spring Boot 4 has not yet been released as of this writing. This project uses Spring Boot **3.5.x**, which is the current LTS-track enterprise standard. The codebase is structured so upgrading to Spring Boot 4 will be trivial when it ships.

---

## Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Run with Docker Compose
```bash
docker compose up --build
```
API available at `http://localhost:8080`  
Swagger UI at `http://localhost:8080/swagger-ui.html`

### Run locally (requires PostgreSQL)
```bash
# Set environment variables or edit application.yml
export DB_HOST=localhost DB_PORT=5432 DB_NAME=taskmanager DB_USERNAME=postgres DB_PASSWORD=postgres

mvn spring-boot:run
```

---

## API Endpoints

| Method   | Path                           | Description                      |
|----------|--------------------------------|----------------------------------|
| `POST`   | `/api/v1/tasks`                | Create a new task                |
| `GET`    | `/api/v1/tasks`                | Get all tasks (paginated)        |
| `GET`    | `/api/v1/tasks/{id}`           | Get task by ID                   |
| `PUT`    | `/api/v1/tasks/{id}`           | Update a task                    |
| `PATCH`  | `/api/v1/tasks/{id}/status`    | Update task status only          |
| `DELETE` | `/api/v1/tasks/{id}`           | Delete a task                    |

### Query Parameters (GET /api/v1/tasks)
| Param     | Type       | Default     | Description             |
|-----------|------------|-------------|-------------------------|
| `status`  | TaskStatus | —           | Filter by status        |
| `title`   | String     | —           | Partial title match     |
| `page`    | int        | 0           | Page number             |
| `size`    | int        | 20          | Page size (max 100)     |
| `sortBy`  | String     | `createdAt` | Sort field              |
| `direction` | DESC/ASC | `DESC`      | Sort direction          |

### Task Status Values
`TODO` | `IN_PROGRESS` | `ON_HOLD` | `DONE` | `CANCELLED`

---

## Running Tests

```bash
# All tests
mvn verify

# Unit tests only (Surefire)
mvn test

# Unit + integration + API tests (Surefire + Failsafe)
mvn verify

# Specific profile
mvn test -Punit-tests
mvn verify -Pintegration-tests

# With coverage report (target/site/jacoco/index.html)
mvn verify jacoco:report
```

### Test Categories
| Type              | Class Pattern          | Runner     | Description                               |
|-------------------|------------------------|------------|-------------------------------------------|
| Unit              | `*Test.java`           | Surefire   | Pure unit tests with Mockito              |
| Controller Unit   | `*ControllerTest.java` | Surefire   | MockMvc slice tests                       |
| Repository        | `*RepositoryTest.java` | Surefire   | `@DataJpaTest` with H2                    |
| Smoke             | `*SmokeTest.java`      | Surefire   | Context loads, actuator health check      |
| Integration       | `*IntegrationTest.java`| Failsafe   | Full Spring context, in-memory DB         |
| API Contract      | `*ApiTest.java`        | Failsafe   | RestAssured MockMvc contract tests        |

---

## Project Structure

```
src/
├── main/java/com/secura/taskmanager/
│   ├── config/          # Spring configuration beans
│   ├── controller/      # REST controllers
│   ├── dto/             # Request/Response records
│   │   ├── request/
│   │   └── response/
│   ├── entity/          # JPA entities & enums
│   ├── exception/       # Custom exceptions & global handler
│   ├── mapper/          # MapStruct mappers
│   ├── repository/      # Spring Data JPA repositories
│   └── service/         # Service interfaces & implementations
├── main/resources/
│   ├── application.yml
│   └── db/migration/    # Flyway migrations
└── test/
    ├── java/...          # All test classes
    └── resources/
        └── application-test.yml
```

---

## Design Decisions

- **Records for DTOs**: Java 21 records ensure immutability and reduce boilerplate.
- **MapStruct**: Zero-reflection compile-time mapping; no runtime performance cost.
- **Optimistic Locking**: `@Version` on entity prevents lost-update anomalies.
- **Flyway**: Schema versioned migrations; `validate` in production.
- **RestAssured MockMvc**: API contract tests without needing a running server.
- **H2 in test mode**: Fast, zero-infrastructure unit/integration tests.
- **JaCoCo**: Enforces 80% line coverage minimum at build time.
