package uk.gov.hmcts.reform.dev.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Paginated response wrapper")
public record PagedResponse<T>(

        @Schema(description = "Page content")
        List<T> content,

        @Schema(description = "Current page number (0-indexed)")
        int pageNumber,

        @Schema(description = "Page size")
        int pageSize,

        @Schema(description = "Total elements across all pages")
        long totalElements,

        @Schema(description = "Total number of pages")
        int totalPages,

        @Schema(description = "Whether this is the last page")
        boolean last,

        @Schema(description = "Whether this is the first page")
        boolean first
) {

    public static <T> PagedResponse<T> from(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst()
        );
    }
}
