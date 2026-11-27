package space.cosmocats.marketplace.product.domain.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import space.cosmocats.marketplace.product.domain.model.Category;

public interface CategoryRepository {
    List<Category> findAll();
    Optional<Category> findById(UUID id);
    Optional<Category> findByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
    Category create(Category category);
    Optional<Category> update(Category category);
    boolean deleteById(UUID id);
    void lockForMutation();
}
