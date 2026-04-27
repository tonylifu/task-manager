package uk.gov.hmcts.reform.dev.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.dev.dto.request.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskStatusRequest;
import uk.gov.hmcts.reform.dev.dto.response.PagedResponse;
import uk.gov.hmcts.reform.dev.dto.response.TaskResponse;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;
import uk.gov.hmcts.reform.dev.exception.GlobalExceptionHandler;
import uk.gov.hmcts.reform.dev.exception.TaskNotFoundException;
import uk.gov.hmcts.reform.dev.service.TaskService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@Tag("unit")
@DisplayName("TaskController Unit Tests")
class TaskControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Mock
    TaskService taskService;

    private UUID taskId;
    private TaskResponse taskResponse;
    private OffsetDateTime futureDate;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        futureDate = OffsetDateTime.now().plusDays(7);
        taskResponse = new TaskResponse(
            taskId, "Test Task", "Description",
            TaskStatus.TODO, futureDate,
            OffsetDateTime.now(), OffsetDateTime.now(), 0L);
    }

    // ─── POST /api/v1/tasks ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/tasks")
    class CreateTask {

        @Test
        @DisplayName("201 - should create task with valid payload")
        void shouldCreateTask() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Test Task", "Description", TaskStatus.TODO, futureDate);

            given(taskService.createTask(any())).willReturn(taskResponse);

            mockMvc.perform(post("/api/v1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(taskId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Test Task"))
                    .andExpect(jsonPath("$.data.status").value("TODO"));
        }

        @Test
        @DisplayName("400 - should reject request with blank title")
        void shouldRejectBlankTitle() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "", "desc", TaskStatus.TODO, futureDate);

            mockMvc.perform(post("/api/v1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("400 - should reject request with null status")
        void shouldRejectNullStatus() throws Exception {
            String json = """
                    {"title":"Valid Title","description":"desc","status":null}""";

            mockMvc.perform(post("/api/v1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - should reject past due date")
        void shouldRejectPastDueDate() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Task", "desc", TaskStatus.TODO,
                    OffsetDateTime.now().minusDays(1));

            mockMvc.perform(post("/api/v1/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── GET /api/v1/tasks/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/tasks/{id}")
    class GetById {

        @Test
        @DisplayName("200 - should return task when found")
        void shouldReturnTask() throws Exception {
            given(taskService.getTaskById(taskId)).willReturn(taskResponse);

            mockMvc.perform(get("/api/v1/tasks/{id}", taskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(taskId.toString()))
                    .andExpect(jsonPath("$.data.title").value("Test Task"));
        }

        @Test
        @DisplayName("404 - should return not found for missing task")
        void shouldReturn404WhenNotFound() throws Exception {
            given(taskService.getTaskById(taskId))
                    .willThrow(new TaskNotFoundException(taskId));

            mockMvc.perform(get("/api/v1/tasks/{id}", taskId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString(taskId.toString())));
        }

        @Test
        @DisplayName("400 - should return bad request for invalid UUID")
        void shouldReturn400ForInvalidUUID() throws Exception {
            mockMvc.perform(get("/api/v1/tasks/not-a-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─── GET /api/v1/tasks ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/tasks")
    class GetAll {

        @Test
        @DisplayName("200 - should return paged tasks")
        void shouldReturnPagedTasks() throws Exception {
            PagedResponse<TaskResponse> paged = new PagedResponse<>(
                    List.of(taskResponse), 0, 20, 1, 1, true, true);

            given(taskService.getAllTasks(isNull(), isNull(), any(Pageable.class)))
                    .willReturn(paged);

            mockMvc.perform(get("/api/v1/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("200 - should apply status filter")
        void shouldApplyStatusFilter() throws Exception {
            PagedResponse<TaskResponse> paged = new PagedResponse<>(
                    List.of(taskResponse), 0, 20, 1, 1, true, true);

            given(taskService.getAllTasks(eq(TaskStatus.TODO), isNull(), any(Pageable.class)))
                    .willReturn(paged);

            mockMvc.perform(get("/api/v1/tasks").param("status", "TODO"))
                    .andExpect(status().isOk());
        }
    }

    // ─── PUT /api/v1/tasks/{id} ────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/v1/tasks/{id}")
    class UpdateTask {

        @Test
        @DisplayName("200 - should update task")
        void shouldUpdateTask() throws Exception {
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Updated", "Updated desc", TaskStatus.IN_PROGRESS, null);
            TaskResponse updated = new TaskResponse(
                    taskId, "Updated", "Updated desc",
                    TaskStatus.IN_PROGRESS, null,
                    OffsetDateTime.now(), OffsetDateTime.now(), 1L);

            given(taskService.updateTask(eq(taskId), any())).willReturn(updated);

            mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Updated"))
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }
    }

    // ─── PATCH /api/v1/tasks/{id}/status ──────────────────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/tasks/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("200 - should update task status")
        void shouldUpdateStatus() throws Exception {
            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.DONE);
            TaskResponse done = new TaskResponse(
                    taskId, "Test Task", null,
                    TaskStatus.DONE, null,
                    OffsetDateTime.now(), OffsetDateTime.now(), 1L);

            given(taskService.updateTaskStatus(eq(taskId), any())).willReturn(done);

            mockMvc.perform(patch("/api/v1/tasks/{id}/status", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DONE"));
        }

        @Test
        @DisplayName("404 - should return not found when task missing")
        void shouldReturn404() throws Exception {
            given(taskService.updateTaskStatus(eq(taskId), any()))
                    .willThrow(new TaskNotFoundException(taskId));

            mockMvc.perform(patch("/api/v1/tasks/{id}/status", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"DONE\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── DELETE /api/v1/tasks/{id} ─────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/v1/tasks/{id}")
    class DeleteTask {

        @Test
        @DisplayName("204 - should delete task")
        void shouldDeleteTask() throws Exception {
            willDoNothing().given(taskService).deleteTask(taskId);

            mockMvc.perform(delete("/api/v1/tasks/{id}", taskId))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("404 - should return not found when task missing")
        void shouldReturn404() throws Exception {
            willThrow(new TaskNotFoundException(taskId))
                    .given(taskService).deleteTask(taskId);

            mockMvc.perform(delete("/api/v1/tasks/{id}", taskId))
                    .andExpect(status().isNotFound());
        }
    }
}
