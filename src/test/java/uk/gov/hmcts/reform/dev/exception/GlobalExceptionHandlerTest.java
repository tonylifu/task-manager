package uk.gov.hmcts.reform.dev.exception;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should return 404 for TaskNotFoundException")
    void shouldHandle404() {
        UUID id = UUID.randomUUID();
        var response = handler.handleTaskNotFound(new TaskNotFoundException(id));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).contains(id.toString());
    }

    @Test
    @DisplayName("Should return 409 for OptimisticLockingFailure")
    void shouldHandle409() {
        var response = handler.handleOptimisticLocking(
                new ObjectOptimisticLockingFailureException("Task", UUID.randomUUID()));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().message()).contains("modified");
    }

    @Test
    @DisplayName("Should return 400 for IllegalArgumentException")
    void shouldHandle400() {
        var response = handler.handleIllegalArgument(
                new IllegalArgumentException("Bad input"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Bad input");
    }

    @Test
    @DisplayName("Should return 400 for Constraint Violation")
    void shouldHandle400ConstraintViolation() {
        var response = handler.handleConstraintViolation(
            new ConstraintViolationException(null));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should return 400 for Http not readable")
    void shouldHandle400HttpNotReadable() {
        var response = handler.handleHttpMessageNotReadable(
            new HttpMessageNotReadableException("Malformed JSON request or invalid enum value"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).isEqualTo("Malformed JSON request or invalid enum value");
    }

    @Test
    @DisplayName("Should return 500 for generic Exception")
    void shouldHandle500() {
        var response = handler.handleGeneral(new RuntimeException("Unexpected"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().success()).isFalse();
    }
}
