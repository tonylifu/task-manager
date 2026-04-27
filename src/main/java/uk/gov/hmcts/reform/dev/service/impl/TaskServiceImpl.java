package uk.gov.hmcts.reform.dev.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import uk.gov.hmcts.reform.dev.service.TaskService;
import uk.gov.hmcts.reform.dev.specification.TaskSpecification;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    @Override
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        log.info("Creating task with title: {}", request.title());
        Task task = taskMapper.toEntity(request);
        Task saved = taskRepository.save(task);
        log.info("Task created with id: {}", saved.getId());
        return taskMapper.toResponse(saved);
    }

    @Override
    public TaskResponse getTaskById(UUID id) {
        log.debug("Fetching task with id: {}", id);
        return taskRepository.findById(id)
                .map(taskMapper::toResponse)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Override
    public PagedResponse<TaskResponse> getAllTasks(TaskStatus status, String title, Pageable pageable) {
        log.debug("Fetching all tasks - status: {}, title: {}, page: {}", status, title, pageable.getPageNumber());
//        Page<Task> tasks = taskRepository.findByFilters(status, title, pageable);
        Specification<Task> spec = Specification
                .allOf(TaskSpecification.hasStatus(status))
                .and(TaskSpecification.titleContains(title));
        Page<Task> tasks = taskRepository.findAll(spec, pageable);
        Page<TaskResponse> responses = tasks.map(taskMapper::toResponse);
        return PagedResponse.from(responses);
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID id, UpdateTaskRequest request) {
        log.info("Updating task with id: {}", id);
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskMapper.updateEntityFromRequest(request, task);
        Task updated = taskRepository.save(task);
        log.info("Task updated: {}", id);
        return taskMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(UUID id, UpdateTaskStatusRequest request) {
        log.info("Updating status of task {} to {}", id, request.status());
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        task.setStatus(request.status());
        Task updated = taskRepository.save(task);
        return taskMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteTask(UUID id) {
        log.info("Deleting task with id: {}", id);
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException(id);
        }
        taskRepository.deleteById(id);
        log.info("Task deleted: {}", id);
    }
}
