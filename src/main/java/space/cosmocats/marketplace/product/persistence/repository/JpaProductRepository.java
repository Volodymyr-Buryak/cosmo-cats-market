package space.cosmocats.marketplace.product.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import space.cosmocats.marketplace.product.persistence.entity.ProductEntity;

public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {
    boolean existsByCategoryIdRefAndNameAndIdNot(UUID categoryId, String name, UUID id);
}
