package space.cosmocats.marketplace.domain.model;

import lombok.*;
import java.util.*;
import space.cosmocats.marketplace.domain.exception.CategoryCannotBeOwnParentException;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Category {
    @EqualsAndHashCode.Include
    private final UUID id;
    private final String name;
    private final String description;
    private final UUID parentId;

    @Builder
    private Category(@NonNull UUID id, @NonNull String name, String description, UUID parentId) {
        String cleanName = name.strip();
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Category name must not be blank");
        }

        if (id.equals(parentId)) {
            throw new CategoryCannotBeOwnParentException(id);
        }

        this.id = id;
        this.name = cleanName;
        this.description = (description == null) ? "" : description.strip();
        this.parentId = parentId;
    }
}