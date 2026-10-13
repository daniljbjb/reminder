package dan.reminder.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Stable public representation of a paginated REST response. */
public record PageResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int number,
        int size,
        boolean first,
        boolean last,
        int numberOfElements,
        boolean empty) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.isFirst(),
                page.isLast(),
                page.getNumberOfElements(),
                page.isEmpty());
    }
}
