package space.cosmocats.marketplace.common.pagination;

import lombok.*;
import java.util.List;
import java.util.function.Function;
import jakarta.validation.constraints.NotNull;

@Builder
public record PageResult<T>(
        @NotNull List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public PageResult {
        content = List.copyOf(content);
    }

    public <R> PageResult<R> map(@NotNull Function<? super T, R> converter) {
        return PageResult.<R>builder()
                .content(
                        content.stream()
                                .map(converter)
                                .toList()
                )
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}