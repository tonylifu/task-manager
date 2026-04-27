package uk.gov.hmcts.reform.dev.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;

import java.time.OffsetDateTime;

@Schema(description = "Request payload to update an existing task")
public record UpdateTaskRequest(

        @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
        @Schema(description = "Task title", example = "Updated task title")
        String title,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        @Schema(description = "Optional task description")
        String description,

        @Schema(description = "Task status", example = "IN_PROGRESS")
        TaskStatus status,

        @Future(message = "Due date must be in the future")
        @Schema(description = "Task due date/time in ISO-8601 format")
        OffsetDateTime dueDate
) {}
