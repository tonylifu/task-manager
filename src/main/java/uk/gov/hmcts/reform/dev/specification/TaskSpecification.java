package uk.gov.hmcts.reform.dev.specification;

import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.reform.dev.entity.Task;
import uk.gov.hmcts.reform.dev.entity.TaskStatus;

public class TaskSpecification {

    public static Specification<Task> hasStatus(final TaskStatus status) {
        return (root, query, cb) ->
            status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Task> titleContains(final String title) {
        return (root, query, cb) ->
            title == null ? null : cb.like(
                cb.lower(root.get("title")),
                "%" + title.toLowerCase() + "%"
            );
    }
}
