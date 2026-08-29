/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.dto;

import java.util.List;
import java.util.function.Function;

/**
 *
 * @author danil
 */
public record PagedResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int number,
        int size,
        boolean first,
        boolean last,
        int numberOfElements,
        boolean empty) {

    public <R> PagedResponse<R> map(Function<T, R> mapper) {
        List<R> newContent = this.content.stream()
                .map(mapper)
                .toList();

        return new PagedResponse<>(
                newContent,
                this.totalPages,
                this.totalElements,
                this.number,
                this.size,
                this.first,
                this.last,
                newContent.size(),
                newContent.isEmpty()
        );
    }
}
