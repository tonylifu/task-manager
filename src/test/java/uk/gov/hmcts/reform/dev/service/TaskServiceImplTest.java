package uk.gov.hmcts.reform.dev.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import uk.gov.hmcts.reform.dev.dto.request.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskStatusRequest;
import uk.gov.hmcts.reform.dev.dto.response.PagedResponse;
import uk.gov.hmcts.reform.dev.dto.response.TaskResponse;
import uk.gov.hmcts.reform.dev.entity.Task;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;
import uk.gov.hmcts.reform.dev.exception.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.mapper.TaskMapper;
import uk.gov.hmcts.reform.dev.repository.TaskRepository;
import uk.gov.hmcts.reform.dev.service.impl.TaskServiceImpl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("TaskService Unit Tests")
class TaskServiceImplTest {

    @Mock
    TaskRepository taskRepository;
    @Mock
    TaskMapper taskMapper;

    @InjectMocks
    TaskServiceImpl taskService;

    private UUID taskId;
    private Task task;
    private TaskResponse taskResponse;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();

        task = Task.builder()
                .id(taskId)
                .title("Test Task")
                .description("Test description")
                .status(TaskStatus.TODO)
                .dueDate(OffsetDateTime.now().plusDays(7))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .version(0L)
                .build();

        taskResponse = new TaskResponse(
                taskId, "Test Task", "Test description",
                TaskStatus.TODO, task.getDueDate(),
                task.getCreatedAt(), task.getUpdatedAt(), 0L);
    }

    // ─── CREATE ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTask")
    class CreateTask {

        @Test
        @DisplayName("should create task and return response")
        void shouldCreateTask() {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Test Task", "Test description",
                    TaskStatus.TODO, OffsetDateTime.now().plusDays(7));

            given(taskMapper.toEntity(request)).willReturn(task);
            given(taskRepository.save(task)).willReturn(task);
            given(taskMapper.toResponse(task)).willReturn(taskResponse);

            TaskResponse result = taskService.createTask(request);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(taskId);
            assertThat(result.title()).isEqualTo("Test Task");
            assertThat(result.status()).isEqualTo(TaskStatus.TODO);

            then(taskRepository).should(times(1)).save(task);
            then(taskMapper).should(times(1)).toEntity(request);
            then(taskMapper).should(times(1)).toResponse(task);
        }

        @Test
        @DisplayName("should create task without optional description")
        void shouldCreateTaskWithoutDescription() {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Minimal Task", null, TaskStatus.TODO, null);

            Task minimalTask = Task.builder()
                    .id(taskId).title("Minimal Task")
                    .status(TaskStatus.TODO).version(0L)
                    .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                    .build();
            TaskResponse minimalResponse = new TaskResponse(
                    taskId, "Minimal Task", null,
                    TaskStatus.TODO, null,
                    minimalTask.getCreatedAt(), minimalTask.getUpdatedAt(), 0L);

            given(taskMapper.toEntity(request)).willReturn(minimalTask);
            given(taskRepository.save(minimalTask)).willReturn(minimalTask);
            given(taskMapper.toResponse(minimalTask)).willReturn(minimalResponse);

            TaskResponse result = taskService.createTask(request);

            assertThat(result.description()).isNull();
            assertThat(result.dueDate()).isNull();
        }
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTaskById")
    class GetTaskById {

        @Test
        @DisplayName("should return task when found")
        void shouldReturnTask() {
            given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
            given(taskMapper.toResponse(task)).willReturn(taskResponse);

            TaskResponse result = taskService.getTaskById(taskId);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(taskId);
        }

        @Test
        @DisplayName("should throw TaskNotFoundException when task does not exist")
        void shouldThrowWhenNotFound() {
            given(taskRepository.findById(taskId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.getTaskById(taskId))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasMessageContaining(taskId.toString());

            then(taskMapper).shouldHaveNoInteractions();
        }
    }

    // ─── GET ALL ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllTasks")
    class GetAllTasks {

        @Test
        @DisplayName("should return paged response")
        void shouldReturnPagedResponse() {
            Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());
            Page<Task> taskPage = new PageImpl<>(List.of(task), pageable, 1);

            given(taskRepository.findByFilters(null, null, pageable)).willReturn(taskPage);
            given(taskMapper.toResponse(task)).willReturn(taskResponse);

            PagedResponse<TaskResponse> result = taskService.getAllTasks(null, null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.pageNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return empty page when no tasks exist")
        void shouldReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Task> emptyPage = Page.empty(pageable);

            given(taskRepository.findByFilters(null, null, pageable)).willReturn(emptyPage);

            PagedResponse<TaskResponse> result = taskService.getAllTasks(null, null, pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("should filter by status")
        void shouldFilterByStatus() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<Task> taskPage = new PageImpl<>(List.of(task));

            given(taskRepository.findByFilters(TaskStatus.TODO, null, pageable))
                    .willReturn(taskPage);
            given(taskMapper.toResponse(task)).willReturn(taskResponse);

            PagedResponse<TaskResponse> result =
                    taskService.getAllTasks(TaskStatus.TODO, null, pageable);

            assertThat(result.content()).hasSize(1);
            then(taskRepository).should().findByFilters(TaskStatus.TODO, null, pageable);
        }
    }

    // ─── UPDATE ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTask")
    class UpdateTask {

        @Test
        @DisplayName("should update task and return updated response")
        void shouldUpdateTask() {
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Updated Title", "Updated desc", TaskStatus.IN_PROGRESS, null);
            TaskResponse updatedResponse = new TaskResponse(
                    taskId, "Updated Title", "Updated desc",
                    TaskStatus.IN_PROGRESS, null,
                    task.getCreatedAt(), OffsetDateTime.now(), 1L);

            given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
            given(taskRepository.save(task)).willReturn(task);
            given(taskMapper.toResponse(task)).willReturn(updatedResponse);

            TaskResponse result = taskService.updateTask(taskId, request);

            assertThat(result.title()).isEqualTo("Updated Title");
            assertThat(result.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            then(taskMapper).should().updateEntityFromRequest(request, task);
        }

        @Test
        @DisplayName("should throw TaskNotFoundException when task does not exist")
        void shouldThrowWhenNotFound() {
            UpdateTaskRequest request = new UpdateTaskRequest("X", null, null, null);
            given(taskRepository.findById(taskId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTask(taskId, request))
                    .isInstanceOf(TaskNotFoundException.class);
        }
    }

    // ─── UPDATE STATUS ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTaskStatus")
    class UpdateTaskStatus {

        @Test
        @DisplayName("should update status successfully")
        void shouldUpdateStatus() {
            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);
            TaskResponse doneResponse = new TaskResponse(
                    taskId, task.getTitle(), task.getDescription(),
                    TaskStatus.DONE, task.getDueDate(),
                    task.getCreatedAt(), OffsetDateTime.now(), 1L);

            given(taskRepository.findById(taskId)).willReturn(Optional.of(task));
            given(taskRepository.save(task)).willReturn(task);
            given(taskMapper.toResponse(task)).willReturn(doneResponse);

            TaskResponse result = taskService.updateTaskStatus(taskId, request);

            assertThat(result.status()).isEqualTo(TaskStatus.DONE);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        @DisplayName("should throw TaskNotFoundException when task does not exist")
        void shouldThrowWhenNotFound() {
            given(taskRepository.findById(taskId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateTaskStatus(
                    taskId, new UpdateTaskStatusRequest(TaskStatus.DONE)))
                    .isInstanceOf(TaskNotFoundException.class);
        }
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteTask")
    class DeleteTask {

        @Test
        @DisplayName("should delete task successfully")
        void shouldDeleteTask() {
            given(taskRepository.existsById(taskId)).willReturn(true);
            willDoNothing().given(taskRepository).deleteById(taskId);

            assertThatCode(() -> taskService.deleteTask(taskId))
                    .doesNotThrowAnyException();

            then(taskRepository).should().deleteById(taskId);
        }

        @Test
        @DisplayName("should throw TaskNotFoundException when task does not exist")
        void shouldThrowWhenNotFound() {
            given(taskRepository.existsById(taskId)).willReturn(false);

            assertThatThrownBy(() -> taskService.deleteTask(taskId))
                    .isInstanceOf(TaskNotFoundException.class);

            then(taskRepository).should(never()).deleteById(any());
        }
    }
}
