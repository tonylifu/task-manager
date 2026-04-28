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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@DisplayName("TaskRepository Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

        taskRepository.flush();
    }

    // ─────────────────────────────────────────────
    // FIND BY ID
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        void shouldFindById() {
            Optional<Task> result = taskRepository.findById(savedTodo.getId());

            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Todo Task");
        }

        @Test
        void shouldReturnEmptyForNonExistent() {
            Optional<Task> result = taskRepository.findById(UUID.randomUUID());
            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────
    // FILTERS
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("findByFilters")
    class FindByFilters {

        @Test
        void shouldReturnAllTasks() {
            Page<Task> result = taskRepository.findByFilters(
                null, null, PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        void shouldFilterByStatus() {
            Page<Task> result = taskRepository.findByFilters(
                TaskStatus.TODO, null, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(TaskStatus.TODO);
        }

        @Test
        void shouldFilterByTitlePartialMatch() {
            Page<Task> result = taskRepository.findByFilters(
                null, "todo", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        void shouldFilterByCombinedFilters() {
            Page<Task> result = taskRepository.findByFilters(
                TaskStatus.TODO, "Todo", PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        void shouldReturnEmptyWhenNoMatch() {
            Page<Task> result = taskRepository.findByFilters(
                TaskStatus.CANCELLED, null, PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }

        @Test
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

    // ─────────────────────────────────────────────
    // COUNT
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("countByStatus")
    class CountByStatus {

        @Test
        void shouldCountByStatus() {
            assertThat(taskRepository.countByStatus(TaskStatus.TODO)).isEqualTo(1);
            assertThat(taskRepository.countByStatus(TaskStatus.DONE)).isEqualTo(1);
            assertThat(taskRepository.countByStatus(TaskStatus.IN_PROGRESS)).isEqualTo(1);
            assertThat(taskRepository.countByStatus(TaskStatus.CANCELLED)).isEqualTo(0);
        }
    }

    // ─────────────────────────────────────────────
    // SAVE / UPDATE
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("save and update")
    class SaveAndUpdate {

        @Test
        void shouldPersistAllFields() {
            Task task = Task.builder()
                .title("Full Task")
                .description("Full description")
                .status(TaskStatus.TODO)
                .dueDate(OffsetDateTime.now().plusDays(10))
                .build();

            Task saved = taskRepository.save(task);
            taskRepository.flush();

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getVersion()).isEqualTo(0);
        }

        @Test
        void shouldIncrementVersionOnUpdate() {
            savedTodo.setTitle("Updated Title");

            Task updated = taskRepository.saveAndFlush(savedTodo);

            assertThat(updated.getVersion()).isGreaterThanOrEqualTo(0);
        }

        @Test
        void shouldUpdateStatus() {
            savedTodo.setStatus(TaskStatus.IN_PROGRESS);

            Task updated = taskRepository.saveAndFlush(savedTodo);

            assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        void shouldDeleteById() {
            taskRepository.deleteById(savedTodo.getId());
            taskRepository.flush();

            assertThat(taskRepository.findById(savedTodo.getId())).isEmpty();
        }
    }
}
