package space.cosmocats.marketplace.common.pagination;

import lombok.*;

import java.util.List;
import java.util.function.Function;

import jakarta.validation.constraints.NotNull;

@Builder
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasContent
) {

    public PageResult {
        content = (content == null) ? List.of() : List.copyOf(content);
        hasContent = !content.isEmpty();
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
