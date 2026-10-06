package space.cosmocats.marketplace.product.persistence.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import space.cosmocats.marketplace.product.persistence.ProductEntity;

public interface JpaProductRepository extends JpaRepository<ProductEntity, UUID> {}
