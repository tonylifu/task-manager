package uk.gov.hmcts.reform.dev.controllers;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.dev.dto.request.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskStatusRequest;
import uk.gov.hmcts.reform.dev.entity.Task;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;
import uk.gov.hmcts.reform.dev.repository.TaskRepository;

import java.time.OffsetDateTime;
import java.util.UUID;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("Task API Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TaskRepository taskRepository;

    private static UUID createdTaskId;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @BeforeEach
    void cleanDatabase() {
        taskRepository.deleteAll();
    }

    // ─── CREATE ────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /api/v1/tasks - 201 with valid payload")
    void shouldCreateTask() {
        CreateTaskRequest request = new CreateTaskRequest(
            "Integration Task",
            "Created in integration test",
            TaskStatus.TODO,
            OffsetDateTime.now().plusDays(5));

        String id = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
        .when()
                .post("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("success", equalTo(true))
                .body("data.id", notNullValue())
                .body("data.title", equalTo("Integration Task"))
                .body("data.status", equalTo("TODO"))
                .body("data.createdAt", notNullValue())
                .extract().path("data.id");

        createdTaskId = UUID.fromString(id);
    }

    @Test
    @DisplayName("POST /api/v1/tasks - 400 when title is blank")
    void shouldRejectBlankTitle() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"title\":\"\",\"status\":\"TODO\"}")
        .when()
                .post("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("success", equalTo(false));
    }

    @Test
    @DisplayName("POST /api/v1/tasks - 400 when status is missing")
    void shouldRejectMissingStatus() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"title\":\"No status task\"}")
        .when()
                .post("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    @DisplayName("POST /api/v1/tasks - 400 with invalid status enum")
    void shouldRejectInvalidStatus() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"title\":\"Task\",\"status\":\"INVALID_STATUS\"}")
        .when()
                .post("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/tasks/{id} - 200 when found")
    void shouldGetTaskById() {
        Task task = taskRepository.save(Task.builder()
                .title("Fetch Me")
                .status(TaskStatus.TODO)
                .build());

        given()
        .when()
                .get("/api/v1/tasks/{id}", task.getId())
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.id", equalTo(task.getId().toString()))
                .body("data.title", equalTo("Fetch Me"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} - 404 when not found")
    void shouldReturn404ForMissingTask() {
        given()
        .when()
                .get("/api/v1/tasks/{id}", UUID.randomUUID())
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("success", equalTo(false));
    }

    // ─── GET ALL ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/v1/tasks - 200 returns paged list")
    void shouldGetAllTasks() {
        taskRepository.save(Task.builder().title("Task 1").status(TaskStatus.TODO).build());
        taskRepository.save(Task.builder().title("Task 2").status(TaskStatus.DONE).build());
        taskRepository.save(Task.builder().title("Task 3").status(TaskStatus.IN_PROGRESS).build());

        given()
        .when()
                .get("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.totalElements", equalTo(3))
                .body("data.content", hasSize(3));
    }

    @Test
    @DisplayName("GET /api/v1/tasks?status=TODO - filters by status")
    void shouldFilterByStatus() {
        taskRepository.save(Task.builder().title("Todo 1").status(TaskStatus.TODO).build());
        taskRepository.save(Task.builder().title("Done 1").status(TaskStatus.DONE).build());

        given()
                .param("status", "TODO")
        .when()
                .get("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.totalElements", equalTo(1))
                .body("data.content[0].status", equalTo("TODO"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks?title=search - filters by title")
    void shouldFilterByTitle() {
        taskRepository.save(Task.builder().title("Special Task Alpha").status(TaskStatus.TODO).build());
        taskRepository.save(Task.builder().title("Other Task Beta").status(TaskStatus.TODO).build());

        given()
                .param("title", "Alpha")
        .when()
                .get("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.totalElements", equalTo(1))
                .body("data.content[0].title", equalTo("Special Task Alpha"));
    }

    @Test
    @DisplayName("GET /api/v1/tasks - respects pagination params")
    void shouldPaginate() {
        for (int i = 1; i <= 5; i++) {
            taskRepository.save(Task.builder()
                    .title("Task " + i).status(TaskStatus.TODO).build());
        }

        given()
                .param("page", 0)
                .param("size", 2)
        .when()
                .get("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.content", hasSize(2))
                .body("data.totalElements", equalTo(5))
                .body("data.totalPages", equalTo(3));
    }

    // ─── PUT ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/v1/tasks/{id} - 200 updates task")
    void shouldUpdateTask() {
        Task task = taskRepository.save(Task.builder()
                .title("Original")
                .status(TaskStatus.TODO)
                .build());

        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated Title", "Updated desc", TaskStatus.IN_PROGRESS, null);

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
        .when()
                .put("/api/v1/tasks/{id}", task.getId())
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.title", equalTo("Updated Title"))
                .body("data.status", equalTo("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PUT /api/v1/tasks/{id} - 404 for missing task")
    void shouldReturn404OnUpdate() {
        UpdateTaskRequest request = new UpdateTaskRequest("X", null, null, null);

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
        .when()
                .put("/api/v1/tasks/{id}", UUID.randomUUID())
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    // ─── PATCH STATUS ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/v1/tasks/{id}/status - 200 updates status")
    void shouldUpdateStatus() {
        Task task = taskRepository.save(Task.builder()
                .title("Status Task")
                .status(TaskStatus.TODO)
                .build());

        UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
        .when()
                .patch("/api/v1/tasks/{id}/status", task.getId())
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.status", equalTo("DONE"));
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} - 204 on success")
    void shouldDeleteTask() {
        Task task = taskRepository.save(Task.builder()
                .title("Delete Me")
                .status(TaskStatus.TODO)
                .build());

        given()
        .when()
                .delete("/api/v1/tasks/{id}", task.getId())
        .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // Verify it's gone
        given()
        .when()
                .get("/api/v1/tasks/{id}", task.getId())
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("DELETE /api/v1/tasks/{id} - 404 for missing task")
    void shouldReturn404OnDelete() {
        given()
        .when()
                .delete("/api/v1/tasks/{id}", UUID.randomUUID())
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }
}
