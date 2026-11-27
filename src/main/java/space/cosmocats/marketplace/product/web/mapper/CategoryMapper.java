package space.cosmocats.marketplace.product.web.mapper;

import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import space.cosmocats.marketplace.product.domain.model.Category;
import space.cosmocats.marketplace.product.web.dto.CategoryRequest;
import space.cosmocats.marketplace.product.web.dto.CategoryResponse;

@Mapper
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);

    @Mapping(target = "id", ignore = true)
    Category toDomain(CategoryRequest request);

    @Mapping(target = "id", source = "id")
    Category toDomain(UUID id, CategoryRequest request);
}
