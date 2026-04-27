package uk.gov.hmcts.reform.dev.repository;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.dev.entity.Task;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("TaskRepository Integration Tests")
class TaskRepositoryTest {

    @Autowired
    TaskRepository taskRepository;

    private Task savedTodo;
    private Task savedDone;
    private Task savedInProgress;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();

        savedTodo = taskRepository.save(Task.builder()
                .title("Todo Task")
                .description("A pending task")
                .status(TaskStatus.TODO)
                .dueDate(OffsetDateTime.now().plusDays(5))
                .build());

        savedDone = taskRepository.save(Task.builder()
                .title("Done Task")
                .description("A completed task")
                .status(TaskStatus.DONE)
                .dueDate(OffsetDateTime.now().plusDays(1))
                .build());

        savedInProgress = taskRepository.save(Task.builder()
                .title("In Progress Task")
                .status(TaskStatus.IN_PROGRESS)
                .build());
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should find existing task by id")
        void shouldFindById() {
            Optional<Task> result = taskRepository.findById(savedTodo.getId());
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Todo Task");
        }

        @Test
        @DisplayName("should return empty for non-existent id")
        void shouldReturnEmptyForNonExistent() {
            Optional<Task> result = taskRepository.findById(java.util.UUID.randomUUID());
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByFilters")
    class FindByFilters {

        @Test
        @DisplayName("should return all tasks when no filters applied")
        void shouldReturnAllTasks() {
            Page<Task> result = taskRepository.findByFilters(
                    null, null, PageRequest.of(0, 10));
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        @DisplayName("should filter by status TODO")
        void shouldFilterByStatus() {
            Page<Task> result = taskRepository.findByFilters(
                    TaskStatus.TODO, null, PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        @DisplayName("should filter by title partial match (case insensitive)")
        void shouldFilterByTitlePartialMatch() {
            Page<Task> result = taskRepository.findByFilters(
                    null, "todo", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Todo Task");
        }

        @Test
        @DisplayName("should filter by both status and title")
        void shouldFilterByCombinedFilters() {
            Page<Task> result = taskRepository.findByFilters(
                    TaskStatus.TODO, "Todo", PageRequest.of(0, 10));
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("should return empty when no match found")
        void shouldReturnEmptyForNoMatch() {
            Page<Task> result = taskRepository.findByFilters(
                    TaskStatus.CANCELLED, null, PageRequest.of(0, 10));
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should honour pagination")
        void shouldHonourPagination() {
            Page<Task> page0 = taskRepository.findByFilters(
                    null, null, PageRequest.of(0, 2));
            Page<Task> page1 = taskRepository.findByFilters(
                    null, null, PageRequest.of(1, 2));

            assertThat(page0.getContent()).hasSize(2);
            assertThat(page1.getContent()).hasSize(1);
            assertThat(page0.getTotalElements()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("countByStatus")
    class CountByStatus {

        @Test
        @DisplayName("should count tasks by status correctly")
        void shouldCountByStatus() {
            assertThat(taskRepository.countByStatus(TaskStatus.TODO)).isEqualTo(1);
            assertThat(taskRepository.countByStatus(TaskStatus.DONE)).isEqualTo(1);
            assertThat(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).isEqualTo(1);
            assertThat(taskRepository.countByStatus(TaskStatus.CANCELLED)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("save and update")
    class SaveAndUpdate {

        @Test
        @DisplayName("should persist all fields correctly")
        void shouldPersistAllFields() {
            OffsetDateTime due = OffsetDateTime.now().plusDays(10);
            Task task = Task.builder()
                    .title("Full Task")
                    .description("Full description")
                    .status(TaskStatus.TODO)
                    .dueDate(due)
                    .build();

            Task saved = taskRepository.save(task);
            taskRepository.flush();

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getVersion()).isEqualTo(0);
        }

        @Test
        @DisplayName("should increment version on update")
        void shouldIncrementVersion() {
            savedTodo.setTitle("Updated Title");
            Task updated = taskRepository.saveAndFlush(savedTodo);
            assertThat(updated.getVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("should update status")
        void shouldUpdateStatus() {
            savedTodo.setStatus(TaskStatus.IN_PROGRESS);
            Task updated = taskRepository.saveAndFlush(savedTodo);
            assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should delete task by id")
        void shouldDeleteById() {
            taskRepository.deleteById(savedTodo.getId());
            taskRepository.flush();
            assertThat(taskRepository.findById(savedTodo.getId())).isEmpty();
        }
    }
}
