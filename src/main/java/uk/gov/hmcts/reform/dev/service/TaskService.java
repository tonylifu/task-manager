package uk.gov.hmcts.reform.dev.service;

import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.dev.dto.request.CreateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskRequest;
import uk.gov.hmcts.reform.dev.dto.request.UpdateTaskStatusRequest;
import uk.gov.hmcts.reform.dev.dto.response.PagedResponse;
import uk.gov.hmcts.reform.dev.dto.response.TaskResponse;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;

import java.util.UUID;

public interface TaskService {

    TaskResponse createTask(CreateTaskRequest request);

    TaskResponse getTaskById(UUID id);

    PagedResponse<TaskResponse> getAllTasks(TaskStatus status, String title, Pageable pageable);

    TaskResponse updateTask(UUID id, UpdateTaskRequest request);

    TaskResponse updateTaskStatus(UUID id, UpdateTaskStatusRequest request);

    void deleteTask(UUID id);
}
