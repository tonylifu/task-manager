package uk.gov.hmcts.reform.dev.api;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * IMPORTANT:
 * Public class + public test methods helps Gradle/JUnit discovery consistently
 * when using custom integrationTest source sets.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("api")
@DisplayName("Task API Contract Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TaskCrudApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    public void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        taskRepository.deleteAll();
    }

    @Test
    @Order(1)
    @DisplayName("Full CRUD lifecycle")
    public void fullCrudLifecycle() {

        CreateTaskRequest request = new CreateTaskRequest(
            "Lifecycle Task",
            "Lifecycle Description",
            TaskStatus.TODO,
            OffsetDateTime.now().plusDays(10)
        );

        String id =
            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .when()
                .post("/api/v1/tasks")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("success", is(true))
                .body("data.title", equalTo("Lifecycle Task"))
                .body("data.status", equalTo("TODO"))
                .extract()
                .path("data.id");

        assertThat(id).isNotBlank();

        given()
            .when()
            .get("/api/v1/tasks/{id}", id)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.id", equalTo(id));

        given()
            .when()
            .get("/api/v1/tasks")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.totalElements", equalTo(1));

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS))
            .when()
            .patch("/api/v1/tasks/{id}/status", id)
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.status", equalTo("IN_PROGRESS"));

        given()
            .when()
            .delete("/api/v1/tasks/{id}", id)
            .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        given()
            .when()
            .get("/api/v1/tasks/{id}", id)
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    @DisplayName("Response envelope contract")
    public void responseEnvelopeContract() {

        CreateTaskRequest request = new CreateTaskRequest(
            "Contract Task",
            null,
            TaskStatus.TODO,
            null
        );

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
            .when()
            .post("/api/v1/tasks")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("success", notNullValue())
            .body("message", notNullValue())
            .body("data", notNullValue())
            .body("timestamp", notNullValue());
    }

    @Test
    @DisplayName("Task response fields contract")
    public void taskResponseFieldContract() {

        CreateTaskRequest request = new CreateTaskRequest(
            "Field Task",
            "Description",
            TaskStatus.IN_PROGRESS,
            null
        );

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body(request)
            .when()
            .post("/api/v1/tasks")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .body("data.id", notNullValue())
            .body("data.title", equalTo("Field Task"))
            .body("data.status", equalTo("IN_PROGRESS"))
            .body("data.createdAt", notNullValue())
            .body("data.updatedAt", notNullValue())
            .body("data.version", notNullValue());
    }

    @Test
    @DisplayName("Paged response contract")
    public void pagedResponseContract() {

        given()
            .when()
            .get("/api/v1/tasks")
            .then()
            .statusCode(HttpStatus.OK.value())
            .body("data.content", notNullValue())
            .body("data.pageNumber", notNullValue())
            .body("data.pageSize", notNullValue())
            .body("data.totalElements", notNullValue())
            .body("data.totalPages", notNullValue())
            .body("data.first", notNullValue())
            .body("data.last", notNullValue());
    }

    @Test
    @DisplayName("404 contract")
    public void errorResponseContract() {

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
    @DisplayName("Validation contract")
    public void validationErrorContract() {

        given()
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .body("""
                {
                  "title":"",
                  "status":"TODO"
                }
                """)
            .when()
            .post("/api/v1/tasks")
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .body("success", is(false))
            .body("data", notNullValue());
    }

    @Test
    @DisplayName("Page size capped at 100")
    public void pageSizeCappedAt100() {

        for (int i = 1; i <= 5; i++) {
            taskRepository.save(
                Task.builder()
                    .title("Task " + i)
                    .description("Desc")
                    .status(TaskStatus.TODO)
                    .build()
            );
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
    @DisplayName("All enum statuses accepted")
    public void allStatusValuesAccepted() {

        for (TaskStatus status : TaskStatus.values()) {

            taskRepository.deleteAll();

            given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(new CreateTaskRequest(
                    "Task " + status.name(),
                    null,
                    status,
                    null
                ))
                .when()
                .post("/api/v1/tasks")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("data.status", equalTo(status.name()));
        }
    }
}
