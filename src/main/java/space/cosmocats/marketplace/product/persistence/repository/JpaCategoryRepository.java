package space.cosmocats.marketplace.product.persistence.repository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import space.cosmocats.marketplace.product.persistence.entity.CategoryEntity;

public interface JpaCategoryRepository extends JpaRepository<CategoryEntity, UUID> {
    Optional<CategoryEntity> findByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
}
