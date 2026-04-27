package uk.gov.hmcts.reform.dev.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;

@Schema(description = "Request payload to update only the task status")
public record UpdateTaskStatusRequest(

        @NotNull(message = "Status is required")
        @Schema(description = "New task status", example = "IN_PROGRESS", requiredMode = Schema.RequiredMode.REQUIRED)
        TaskStatus status
) {}
