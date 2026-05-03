package uk.gov.hmcts.reform.dev.mapper;

import org.junit.jupiter.api.*;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.dev.dto.request.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.response.TaskResponse;
import uk.gov.hmcts.reform.dev.entity.Task;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Tag("unit")
@DisplayName("TaskMapper Unit Tests")
class TaskMapperTest {

    private TaskMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TaskMapperImpl();
    }

    // ─────────────────────────────────────────────
    // toEntity
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("toEntity(CreateTaskRequest)")
    class ToEntity {

        @Test
        @DisplayName("should return null when request is null")
        void shouldReturnNullWhenRequestIsNull() {
            assertThat(mapper.toEntity(null)).isNull();
        }

        @Test
        @DisplayName("should map all fields correctly")
        void shouldMapAllFields() {
            OffsetDateTime dueDate = OffsetDateTime.of(2026, 6, 1, 12, 0,
                                                       0, 0, ZoneOffset.UTC);
            CreateTaskRequest request = new CreateTaskRequest(
                    "Write tests",
                    "Cover the mapper",
                    TaskStatus.TODO,
                    dueDate
            );

            Task result = mapper.toEntity(request);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Write tests");
            assertThat(result.getDescription()).isEqualTo("Cover the mapper");
            assertThat(result.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(result.getDueDate()).isEqualTo(dueDate);
        }

        @Test
        @DisplayName("should map with null optional fields")
        void shouldMapWithNullOptionalFields() {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Title only",
                    null,
                    TaskStatus.TODO,
                    null
            );

            Task result = mapper.toEntity(request);

            assertThat(result).isNotNull();
            assertThat(result.getTitle()).isEqualTo("Title only");
            assertThat(result.getDescription()).isNull();
            assertThat(result.getDueDate()).isNull();
        }

        @Test
        @DisplayName("should not set auditing fields (id, createdAt, updatedAt, version)")
        void shouldNotSetAuditingFields() {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Audit check", "desc", TaskStatus.IN_PROGRESS, null);

            Task result = mapper.toEntity(request);

            assertThat(result.getId()).isNull();
            assertThat(result.getCreatedAt()).isNull();
            assertThat(result.getUpdatedAt()).isNull();
            assertThat(result.getVersion()).isNull();
        }
    }

    // ─────────────────────────────────────────────
    // toResponse
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("toResponse(Task)")
    class ToResponse {

        @Test
        @DisplayName("should return null when task is null")
        void shouldReturnNullWhenTaskIsNull() {
            assertThat(mapper.toResponse(null)).isNull();
        }

        @Test
        @DisplayName("should map all fields correctly")
        void shouldMapAllFields() {
            UUID id = UUID.randomUUID();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime dueDate = now.plusDays(7);

            Task task = Task.builder()
                    .id(id)
                    .title("Deploy service")
                    .description("Push to prod")
                    .status(TaskStatus.IN_PROGRESS)
                    .dueDate(dueDate)
                    .createdAt(now)
                    .updatedAt(now)
                    .version(1L)
                    .build();

            TaskResponse result = mapper.toResponse(task);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(id);
            assertThat(result.title()).isEqualTo("Deploy service");
            assertThat(result.description()).isEqualTo("Push to prod");
            assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(result.dueDate()).isEqualTo(dueDate);
            assertThat(result.createdAt()).isEqualTo(now);
            assertThat(result.updatedAt()).isEqualTo(now);
            assertThat(result.version()).isEqualTo(1L);
        }

        @Test
        @DisplayName("should map with null optional fields")
        void shouldMapWithNullOptionalFields() {
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Minimal task")
                    .status(TaskStatus.TODO)
                    .build();

            TaskResponse result = mapper.toResponse(task);

            assertThat(result).isNotNull();
            assertThat(result.description()).isNull();
            assertThat(result.dueDate()).isNull();
            assertThat(result.createdAt()).isNull();
            assertThat(result.updatedAt()).isNull();
            assertThat(result.version()).isNull();
        }
    }

    // ─────────────────────────────────────────────
    // updateEntityFromRequest
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("updateEntityFromRequest(UpdateTaskRequest, Task)")
    class UpdateEntityFromRequest {

        private Task existingTask;

        @BeforeEach
        void setUp() {
            existingTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Original title")
                    .description("Original description")
                    .status(TaskStatus.TODO)
                    .dueDate(OffsetDateTime.of(2026, 1, 1, 0, 0, 0,
                                               0, ZoneOffset.UTC))
                    .build();
        }

        @Test
        @DisplayName("should do nothing when request is null")
        void shouldDoNothingWhenRequestIsNull() {
            mapper.updateEntityFromRequest(null, existingTask);

            assertThat(existingTask.getTitle()).isEqualTo("Original title");
            assertThat(existingTask.getDescription()).isEqualTo("Original description");
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        @DisplayName("should update all fields when all are provided")
        void shouldUpdateAllFields() {
            OffsetDateTime newDueDate = OffsetDateTime.of(2026, 12, 31, 0, 0,
                                                          0, 0, ZoneOffset.UTC);
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Updated title",
                    "Updated description",
                    TaskStatus.DONE,
                    newDueDate
            );

            mapper.updateEntityFromRequest(request, existingTask);

            assertThat(existingTask.getTitle()).isEqualTo("Updated title");
            assertThat(existingTask.getDescription()).isEqualTo("Updated description");
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.DONE);
            assertThat(existingTask.getDueDate()).isEqualTo(newDueDate);
        }

        @Test
        @DisplayName("should only update title when other fields are null")
        void shouldOnlyUpdateTitle() {
            UpdateTaskRequest request = new UpdateTaskRequest("New title", null, null, null);

            mapper.updateEntityFromRequest(request, existingTask);

            assertThat(existingTask.getTitle()).isEqualTo("New title");
            assertThat(existingTask.getDescription()).isEqualTo("Original description");
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(existingTask.getDueDate()).isEqualTo(
                    OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        }

        @Test
        @DisplayName("should only update status when other fields are null")
        void shouldOnlyUpdateStatus() {
            UpdateTaskRequest request = new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null);

            mapper.updateEntityFromRequest(request, existingTask);

            assertThat(existingTask.getTitle()).isEqualTo("Original title");
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(existingTask.getDescription()).isEqualTo("Original description");
        }

        @Test
        @DisplayName("should only update dueDate when other fields are null")
        void shouldOnlyUpdateDueDate() {
            OffsetDateTime newDueDate = OffsetDateTime.of(2027, 3, 15, 9, 0,
                                                          0, 0, ZoneOffset.UTC);
            UpdateTaskRequest request = new UpdateTaskRequest(null, null, null, newDueDate);

            mapper.updateEntityFromRequest(request, existingTask);

            assertThat(existingTask.getDueDate()).isEqualTo(newDueDate);
            assertThat(existingTask.getTitle()).isEqualTo("Original title");
            assertThat(existingTask.getDescription()).isEqualTo("Original description");
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        @DisplayName("should not modify task id or auditing fields during update")
        void shouldNotModifyAuditingFields() {
            UUID originalId = existingTask.getId();
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Changed", "Changed", TaskStatus.DONE, null);

            mapper.updateEntityFromRequest(request, existingTask);

            assertThat(existingTask.getId()).isEqualTo(originalId);
        }
    }
}
