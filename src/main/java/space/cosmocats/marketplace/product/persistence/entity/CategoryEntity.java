package space.cosmocats.marketplace.product.persistence.entity;

import lombok.*;
import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.annotations.NaturalId;
import space.cosmocats.marketplace.common.persistence.BaseEntity;

@Entity
@Table(name = "categories", schema = "public")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryEntity extends BaseEntity {
    @NaturalId(mutable = true)
    private String name;

    private String description;
    private UUID parentId;

    @Builder
    public CategoryEntity(UUID id, String name, String description, UUID parentId) {
        super(id);
        this.name = name;
        this.description = description;
        this.parentId = parentId;
    }
}
