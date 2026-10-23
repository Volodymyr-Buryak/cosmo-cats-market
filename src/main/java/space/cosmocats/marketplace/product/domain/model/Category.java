package space.cosmocats.marketplace.product.domain.model;

import lombok.*;
import java.util.*;
import space.cosmocats.marketplace.product.domain.exception.CategoryCannotBeOwnParentException;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {
    @EqualsAndHashCode.Include
    UUID id;
    String name;
    String description;
    UUID parentId;

    @Builder(toBuilder = true)
    private Category(UUID id, @NonNull String name, @NonNull String description, UUID parentId) {
        this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        this.name = validateName(name);
        this.description = validateDescription(description);

        if (this.id.equals(parentId)) {
            throw new CategoryCannotBeOwnParentException(this.id, this.name);
        }

        this.parentId = parentId;
    }

    private static String validateName(String name) {
        String normalized = name.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        return normalized;
    }

    private static String validateDescription(String description) {
        String normalized = description.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Category description must not be blank");
        }
        return normalized;
    }
}
