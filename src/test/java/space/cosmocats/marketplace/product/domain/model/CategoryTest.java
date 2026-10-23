package space.cosmocats.marketplace.product.domain.model;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import space.cosmocats.marketplace.product.domain.exception.CategoryCannotBeOwnParentException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CategoryTest {

    private static final String CATEGORY_NAME = "Cosmic equipment";
    private static final String CATEGORY_DESCRIPTION = "Equipment for space travel";
    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PARENT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    @DisplayName("Preserves the provided category ID")
    void shouldPreserveProvidedId() {
        // Given
        var builder = validCategoryBuilder().id(CATEGORY_ID);

        // When
        Category category = builder.build();

        // Then
        assertThat(category.getId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    @DisplayName("Generates a category ID when none is provided")
    void shouldGenerateIdWhenNotProvided() {
        var builder = validCategoryBuilder();

        Category category = builder.build();

        assertThat(category.getId()).isNotNull();
    }

    @Test
    @DisplayName("Strips leading and trailing whitespace from the name")
    void shouldStripLeadingAndTrailingWhitespaceFromName() {
        var builder = validCategoryBuilder().name(" \tCosmic equipment\n ");

        Category category = builder.build();

        assertThat(category.getName()).isEqualTo(CATEGORY_NAME);
    }

    @Test
    @DisplayName("Strips leading and trailing whitespace from the description")
    void shouldStripLeadingAndTrailingWhitespaceFromDescription() {
        var builder = validCategoryBuilder().description(" \tEquipment for space travel\n ");

        Category category = builder.build();

        assertThat(category.getDescription()).isEqualTo(CATEGORY_DESCRIPTION);
    }

    @ParameterizedTest
    @DisplayName("Rejects an empty or whitespace-only name")
    @ValueSource(strings = {"", "   ", "\t\n"})
    void shouldRejectBlankName(String name) {
        var builder = validCategoryBuilder().name(name);

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @DisplayName("Rejects an empty or whitespace-only description")
    @ValueSource(strings = {"", "   ", "\t\n"})
    void shouldRejectBlankDescription(String description) {
        var builder = validCategoryBuilder().description(description);

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "[{index}] missing field = {0}")
    @DisplayName("Rejects building a category without a required field")
    @MethodSource("buildersWithMissingRequiredFields")
    void shouldRejectBuildingCategoryWithoutRequiredField(String fieldName, Category.CategoryBuilder builder) {
        Throwable exception = catchThrowable(builder::build);

        assertThat(exception)
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessageContaining(fieldName);
    }

    @Test
    @DisplayName("Allows creating a root category without a parent")
    void shouldAllowCategoryWithoutParent() {
        var builder = validCategoryBuilder().parentId(null);

        Category category = builder.build();

        assertThat(category.getParentId()).isNull();
    }

    @Test
    @DisplayName("Preserves the ID of a different parent category")
    void shouldPreserveParentId() {
        var builder = validCategoryBuilder().id(CATEGORY_ID).parentId(PARENT_ID);

        Category category = builder.build();

        assertThat(category.getParentId()).isEqualTo(PARENT_ID);
    }

    @Test
    @DisplayName("Rejects a category assigned as its own parent")
    void shouldRejectCategoryAsOwnParent() {
        var builder = validCategoryBuilder().id(CATEGORY_ID).parentId(CATEGORY_ID);

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isInstanceOf(CategoryCannotBeOwnParentException.class);
    }

    @Test
    @DisplayName("Rebuilds a category with the same ID without changing the original")
    void shouldRebuildCategoryWithoutChangingOriginal() {
        Category originalCategory = validCategoryBuilder().id(CATEGORY_ID).parentId(PARENT_ID).build();
        UUID newParentId = UUID.fromString("00000000-0000-0000-0000-000000000003");

        Category updatedCategory = originalCategory.toBuilder()
                .name("Navigation")
                .description("Equipment for interstellar navigation")
                .parentId(newParentId)
                .build();

        assertThat(updatedCategory).isNotSameAs(originalCategory);
        assertThat(updatedCategory.getId()).isEqualTo(originalCategory.getId());
        assertThat(updatedCategory.getName()).isEqualTo("Navigation");
        assertThat(updatedCategory.getDescription()).isEqualTo("Equipment for interstellar navigation");
        assertThat(updatedCategory.getParentId()).isEqualTo(newParentId);
        assertThat(originalCategory.getName()).isEqualTo(CATEGORY_NAME);
        assertThat(originalCategory.getDescription()).isEqualTo(CATEGORY_DESCRIPTION);
        assertThat(originalCategory.getParentId()).isEqualTo(PARENT_ID);
    }

    @Test
    @DisplayName("Rejects assigning a category as its own parent through toBuilder")
    void shouldRejectOwnParentWhenRebuildingCategory() {
        Category originalCategory = validCategoryBuilder().id(CATEGORY_ID).parentId(PARENT_ID).build();
        var builder = originalCategory.toBuilder().parentId(originalCategory.getId());

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isInstanceOf(CategoryCannotBeOwnParentException.class);
        assertThat(originalCategory.getParentId()).isEqualTo(PARENT_ID);
    }

    private static Stream<Arguments> buildersWithMissingRequiredFields() {
        return Stream.of(
                Arguments.of("name", Category.builder().description(CATEGORY_DESCRIPTION)),
                Arguments.of("description", Category.builder().name(CATEGORY_NAME))
        );
    }

    private static Category.CategoryBuilder validCategoryBuilder() {
        return Category.builder()
                .name(CATEGORY_NAME)
                .description(CATEGORY_DESCRIPTION);
    }
}
