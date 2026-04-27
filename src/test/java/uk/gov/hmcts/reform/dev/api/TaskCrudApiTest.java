package uk.gov.hmcts.reform.dev.api;

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
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskStatusRequest;
import uk.gov.hmcts.reform.dev.entity.Task;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;
import uk.gov.hmcts.reform.dev.repository.TaskRepository;

import java.time.OffsetDateTime;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * RestAssured API contract tests.
 * Named *ApiTest so Failsafe picks them up separately from Surefire unit tests.
 * These verify API contracts: status codes, response body shapes, and header values.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("api")
@DisplayName("Task API Contract Tests (RestAssured)")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TaskCrudApiTest {

    @Autowired MockMvc mockMvc;
    @Autowired
    TaskRepository taskRepository;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        taskRepository.deleteAll();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Full CRUD lifecycle as a sequence
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("Full CRUD lifecycle: create → read → update status → delete")
    void fullCrudLifecycle() {
        // 1. CREATE
        CreateTaskRequest createReq = new CreateTaskRequest(
            "Lifecycle Task", "A full lifecycle task",
            TaskStatus.TODO, OffsetDateTime.now().plusDays(10));

        String taskId = given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(createReq)
        .when()
                .post("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("success", is(true))
                .body("data.title", equalTo("Lifecycle Task"))
                .body("data.status", equalTo("TODO"))
                .body("data.version", equalTo(0))
                .extract().path("data.id");

        assertThat(taskId).isNotNull();

        // 2. READ BY ID
        given()
        .when()
                .get("/api/v1/tasks/{id}", taskId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.id", equalTo(taskId))
                .body("data.title", equalTo("Lifecycle Task"));

        // 3. READ ALL — should contain our task
        given()
        .when()
                .get("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.totalElements", equalTo(1));

        // 4. UPDATE STATUS
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS))
        .when()
                .patch("/api/v1/tasks/{id}/status", taskId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.status", equalTo("IN_PROGRESS"));

        // 5. VERIFY STATUS CHANGED
        given()
        .when()
                .get("/api/v1/tasks/{id}", taskId)
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.status", equalTo("IN_PROGRESS"));

        // 6. DELETE
        given()
        .when()
                .delete("/api/v1/tasks/{id}", taskId)
        .then()
                .statusCode(HttpStatus.NO_CONTENT.value());

        // 7. CONFIRM DELETED
        given()
        .when()
                .get("/api/v1/tasks/{id}", taskId)
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Response body contract assertions
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Response envelope always contains success, message, data, timestamp")
    void responseEnvelopeContract() {
        CreateTaskRequest req = new CreateTaskRequest(
                "Contract Task", null, TaskStatus.TODO, null);

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(req)
        .when()
                .post("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("success",   notNullValue())
                .body("message",   notNullValue())
                .body("data",      notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    @DisplayName("Task response always includes all required fields")
    void taskResponseFieldContract() {
        CreateTaskRequest req = new CreateTaskRequest(
                "Field Contract Task", "Description", TaskStatus.IN_PROGRESS, null);

        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(req)
        .when()
                .post("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("data.id",        notNullValue())
                .body("data.title",     equalTo("Field Contract Task"))
                .body("data.status",    equalTo("IN_PROGRESS"))
                .body("data.createdAt", notNullValue())
                .body("data.updatedAt", notNullValue())
                .body("data.version",   notNullValue());
    }

    @Test
    @DisplayName("Paged response has required pagination metadata fields")
    void pagedResponseContract() {
        given()
        .when()
                .get("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.content",      notNullValue())
                .body("data.pageNumber",   notNullValue())
                .body("data.pageSize",     notNullValue())
                .body("data.totalElements",notNullValue())
                .body("data.totalPages",   notNullValue())
                .body("data.first",        notNullValue())
                .body("data.last",         notNullValue());
    }

    @Test
    @DisplayName("Error response has success=false and message for 404")
    void errorResponseContract() {
        given()
        .when()
                .get("/api/v1/tasks/00000000-0000-0000-0000-000000000000")
        .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("success", is(false))
                .body("message", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    @DisplayName("Validation error response lists field-level errors")
    void validationErrorContract() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"title\":\"\",\"status\":\"TODO\"}")
        .when()
                .post("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("success", is(false))
                .body("data.title", notNullValue());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Edge cases
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Page size is capped at 100")
    void pageSizeCappedAt100() {
        for (int i = 1; i <= 5; i++) {
            taskRepository.save(Task.builder()
                    .title("Task " + i).status(TaskStatus.TODO).build());
        }

        given()
                .param("size", 9999)
        .when()
                .get("/api/v1/tasks")
        .then()
                .statusCode(HttpStatus.OK.value())
                .body("data.pageSize", lessThanOrEqualTo(100));
    }

    @Test
    @DisplayName("All TaskStatus enum values are accepted on create")
    void allStatusValuesAccepted() {
        for (TaskStatus status : TaskStatus.values()) {
            taskRepository.deleteAll();
            given()
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
                    .body(new CreateTaskRequest("Task for " + status, null, status, null))
            .when()
                    .post("/api/v1/tasks")
            .then()
                    .statusCode(HttpStatus.CREATED.value())
                    .body("data.status", equalTo(status.getValue()));
        }
    }
}
