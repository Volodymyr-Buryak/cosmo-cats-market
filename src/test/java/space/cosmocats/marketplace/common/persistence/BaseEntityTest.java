package space.cosmocats.marketplace.common.persistence;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import space.cosmocats.marketplace.product.persistence.entity.CategoryEntity;
import space.cosmocats.marketplace.product.persistence.entity.ProductEntity;

import static org.assertj.core.api.Assertions.assertThat;

class BaseEntityTest {

    private static final UUID ENTITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_ENTITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    @DisplayName("Considers an entity equal to itself")
    void shouldEqualItself() {
        // Given
        CategoryEntity entity = CategoryEntity.builder().id(ENTITY_ID).build();

        // When
        boolean result = entity.equals(entity);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Compares entities of the same type by ID regardless of other fields")
    void shouldCompareSameTypeById() {
        CategoryEntity first = CategoryEntity.builder().id(ENTITY_ID).name("First").build();
        CategoryEntity second = CategoryEntity.builder().id(ENTITY_ID).name("Second").build();

        boolean result = first.equals(second);

        assertThat(result).isTrue();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    @DisplayName("Distinguishes entities with different IDs")
    void shouldDistinguishDifferentIds() {
        CategoryEntity first = CategoryEntity.builder().id(ENTITY_ID).build();
        CategoryEntity second = CategoryEntity.builder().id(OTHER_ENTITY_ID).build();

        boolean result = first.equals(second);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Distinguishes different entity types even when their IDs match")
    void shouldDistinguishDifferentEntityTypes() {
        CategoryEntity category = CategoryEntity.builder().id(ENTITY_ID).build();
        ProductEntity product = ProductEntity.builder().id(ENTITY_ID).build();

        boolean result = category.equals(product);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Does not consider an entity equal to null or an unrelated object")
    void shouldRejectUnrelatedObjects() {
        CategoryEntity entity = CategoryEntity.builder().id(ENTITY_ID).build();

        boolean equalsNull = entity.equals(null);
        boolean equalsString = entity.equals("category");

        assertThat(equalsNull).isFalse();
        assertThat(equalsString).isFalse();
    }

    @Test
    @DisplayName("Does not consider distinct transient entities equal")
    void shouldDistinguishTransientEntities() {
        CategoryEntity transientEntity = CategoryEntity.builder().build();
        CategoryEntity otherTransientEntity = CategoryEntity.builder().build();
        CategoryEntity persistedEntity = CategoryEntity.builder().id(ENTITY_ID).build();

        boolean equalsTransient = transientEntity.equals(otherTransientEntity);
        boolean equalsPersisted = transientEntity.equals(persistedEntity);

        assertThat(equalsTransient).isFalse();
        assertThat(equalsPersisted).isFalse();
    }
}
