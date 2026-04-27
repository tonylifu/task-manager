package uk.gov.hmcts.reform.dev.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Task response payload")
public record TaskResponse(

        @Schema(description = "Unique task identifier")
        UUID id,

        @Schema(description = "Task title")
        String title,

        @Schema(description = "Task description")
        String description,

        @Schema(description = "Task status")
        TaskStatus status,

        @Schema(description = "Task due date/time")
        OffsetDateTime dueDate,

        @Schema(description = "Task creation timestamp")
        OffsetDateTime createdAt,

        @Schema(description = "Task last update timestamp")
        OffsetDateTime updatedAt,

        @Schema(description = "Optimistic locking version")
        Long version
) {}
