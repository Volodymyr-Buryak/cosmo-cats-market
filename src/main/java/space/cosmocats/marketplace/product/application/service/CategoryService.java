package space.cosmocats.marketplace.product.application.service;

import java.util.List;
import java.util.UUID;
import space.cosmocats.marketplace.product.domain.model.Category;

public interface CategoryService {
    List<Category> getAllCategories();
    Category getCategoryById(UUID id);
    java.util.Optional<Category> findCategoryByName(String name);
    Category createCategory(Category category);
    Category updateCategory(Category category);
    void deleteCategory(UUID id);
}
