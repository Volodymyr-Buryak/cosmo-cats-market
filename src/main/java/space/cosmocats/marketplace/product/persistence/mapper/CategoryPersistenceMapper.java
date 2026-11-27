package space.cosmocats.marketplace.product.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import space.cosmocats.marketplace.product.domain.model.Category;
import space.cosmocats.marketplace.product.persistence.entity.CategoryEntity;

@Mapper
public interface CategoryPersistenceMapper {
    Category toDomain(CategoryEntity entity);
    CategoryEntity toEntity(Category category);

    @Mapping(target = "id", ignore = true)
    void updateEntity(Category category, @MappingTarget CategoryEntity entity);
}
