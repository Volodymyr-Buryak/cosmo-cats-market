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
    private Category(UUID id, @NonNull String name, String description, UUID parentId) {
        this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);

        if (this.id.equals(parentId)) {
            throw new CategoryCannotBeOwnParentException(this.id);
        }

        this.name = validateName(name);
        this.description = (description == null) ? "" : description.strip();
        this.parentId = parentId;
    }

    private static String validateName(String name) {
        String normalized = name.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }
        return normalized;
    }
}