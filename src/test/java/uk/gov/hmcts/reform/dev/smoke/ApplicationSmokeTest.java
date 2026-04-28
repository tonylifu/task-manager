package uk.gov.hmcts.reform.dev.smoke;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.dev.controller.TaskController;
import uk.gov.hmcts.reform.dev.repository.TaskRepository;
import uk.gov.hmcts.reform.dev.api.TaskService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests — verify the application context loads correctly
 * and critical beans / endpoints are reachable.
 * These are intentionally lightweight and should always be the first gate.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Tag("smoke")
@DisplayName("Application Smoke Tests")
class ApplicationSmokeTest {

    @LocalServerPort
    private int port;

    @Autowired(required = false) private TaskController taskController;
    @Autowired(required = false) private TaskService taskService;
    @Autowired(required = false) private TaskRepository taskRepository;
    @Autowired private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Application context loads without errors")
    void contextLoads() {
        // If the context fails to load the test itself fails — no assertion needed
    }

    @Test
    @DisplayName("TaskController bean is present in context")
    void taskControllerBeanLoaded() {
        assertThat(taskController).isNotNull();
    }

    @Test
    @DisplayName("TaskService bean is present in context")
    void taskServiceBeanLoaded() {
        assertThat(taskService).isNotNull();
    }

    @Test
    @DisplayName("TaskRepository bean is present in context")
    void taskRepositoryBeanLoaded() {
        assertThat(taskRepository).isNotNull();
    }

    @Test
    @DisplayName("Actuator /health endpoint responds 200")
    void healthEndpointResponds() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    @DisplayName("Tasks API endpoint is accessible and returns 200")
    void tasksApiEndpointAccessible() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/tasks", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Invalid endpoint returns 404 not 500")
    void invalidEndpointReturns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/nonexistent", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
