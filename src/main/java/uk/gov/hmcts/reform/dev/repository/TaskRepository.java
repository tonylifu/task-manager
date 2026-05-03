package uk.gov.hmcts.reform.dev.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.dev.entity.Task;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    Page<Task> findAllByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findAllByDueDateBefore(OffsetDateTime dateTime, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:title IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%')))")
    Page<Task> findByFilters(@Param("status") TaskStatus status,
                             @Param("title") String title,
                             Pageable pageable);

    long countByStatus(TaskStatus status);

    @Query("SELECT t.status, COUNT(t) FROM Task t GROUP BY t.status")
    List<Object[]> countGroupByStatus();

    boolean existsByTitleIgnoreCase(String title);
}
