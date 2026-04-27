package uk.gov.hmcts.reform.dev.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;

import java.time.OffsetDateTime;

@Schema(description = "Request payload to create a new task")
public record CreateTaskRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
        @Schema(description = "Task title", example = "Implement login feature", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        @Schema(description = "Optional task description", example = "Implement OAuth2 login with Google")
        String description,

        @NotNull(message = "Status is required")
        @Schema(description = "Task status", example = "TODO", requiredMode = Schema.RequiredMode.REQUIRED)
        TaskStatus status,

        @Future(message = "Due date must be in the future")
        @Schema(description = "Task due date/time in ISO-8601 format", example = "2025-12-31T23:59:59Z")
        OffsetDateTime dueDate
) {}
