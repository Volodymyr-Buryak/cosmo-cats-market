package space.cosmocats.marketplace.common.pagination;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PageResultTest {

    @Test
    @DisplayName("Preserves content and pagination metadata and calculates hasContent")
    void shouldPreserveContentAndPaginationMetadata() {
        // Given
        List<String> content = List.of("star", "galaxy", "comet");

        // When
        PageResult<String> page = new PageResult<>(content, 2, 3, 10L, 4, false);

        // Then
        assertThat(page.content()).containsExactly("star", "galaxy", "comet");
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(3);
        assertThat(page.totalElements()).isEqualTo(10L);
        assertThat(page.totalPages()).isEqualTo(4);
        assertThat(page.hasContent()).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Normalizes null and empty content and recalculates hasContent")
    @NullAndEmptySource
    void shouldNormalizeEmptyContent(List<String> content) {
        PageResult<String> page = new PageResult<>(content, 0, 3, 0L, 0, true);

        assertThat(page.content()).isEmpty();
        assertThat(page.hasContent()).isFalse();
    }

    @Test
    @DisplayName("Copies content so later changes to the source list do not affect the page")
    void shouldDefensivelyCopyContent() {
        List<String> content = new ArrayList<>(List.of("star", "galaxy"));
        PageResult<String> page = pageWithContent(content);

        content.clear();

        assertThat(page.content()).containsExactly("star", "galaxy");
        assertThat(page.hasContent()).isTrue();
    }

    @Test
    @DisplayName("Exposes unmodifiable content")
    void shouldPreventModifyingContent() {
        PageResult<String> page = pageWithContent(List.of("star"));

        Throwable exception = catchThrowable(() -> page.content().add("galaxy"));

        assertThat(exception).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThat(page.content()).containsExactly("star");
    }

    @Test
    @DisplayName("Rejects null elements in content")
    void shouldRejectNullContentElement() {
        List<String> content = Arrays.asList("star", null);

        Throwable exception = catchThrowable(() -> pageWithContent(content));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Maps every item in order while preserving metadata and the original page")
    void shouldMapContentAndPreserveMetadata() {
        PageResult<String> page = pageWithContent(List.of("12", "5", "12"));

        PageResult<Integer> mappedPage = page.map(Integer::parseInt);

        assertThat(mappedPage).isNotSameAs(page);
        assertThat(mappedPage.content()).containsExactly(12, 5, 12);
        assertThat(mappedPage.page()).isEqualTo(page.page());
        assertThat(mappedPage.size()).isEqualTo(page.size());
        assertThat(mappedPage.totalElements()).isEqualTo(page.totalElements());
        assertThat(mappedPage.totalPages()).isEqualTo(page.totalPages());
        assertThat(mappedPage.hasContent()).isTrue();
        assertThat(page.content()).containsExactly("12", "5", "12");
    }

    @Test
    @DisplayName("Maps an empty page without invoking the converter and preserves metadata")
    void shouldMapEmptyPageWithoutInvokingConverter() {
        PageResult<String> page = new PageResult<>(List.of(), 4, 3, 10L, 4, false);

        PageResult<Integer> mappedPage = page.map(value -> {
            throw new AssertionError("The converter must not be invoked for empty content");
        });

        assertThat(mappedPage.content()).isEmpty();
        assertThat(mappedPage.hasContent()).isFalse();
        assertThat(mappedPage.page()).isEqualTo(page.page());
        assertThat(mappedPage.size()).isEqualTo(page.size());
        assertThat(mappedPage.totalElements()).isEqualTo(page.totalElements());
        assertThat(mappedPage.totalPages()).isEqualTo(page.totalPages());
    }

    @ParameterizedTest(name = "[{index}] empty content = {0}")
    @DisplayName("Rejects a null converter for both empty and nonempty pages")
    @ValueSource(booleans = {true, false})
    void shouldRejectNullConverter(boolean emptyContent) {
        List<String> content = emptyContent ? List.of() : List.of("star");
        PageResult<String> page = pageWithContent(content);

        Throwable exception = catchThrowable(() -> page.map(null));

        assertThat(exception)
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessageContaining("converter");
    }

    private static <T> PageResult<T> pageWithContent(List<T> content) {
        return PageResult.<T>builder()
                .content(content)
                .page(2)
                .size(3)
                .totalElements(10L)
                .totalPages(4)
                .build();
    }
}
