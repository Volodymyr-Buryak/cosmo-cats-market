package space.cosmocats.marketplace.product.domain.repository;

import java.util.Optional;
import java.util.UUID;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.common.pagination.PageResult;

public interface ProductRepository {
    Optional<Product> findById(UUID id);
    Product save(Product product);
    Optional<Product> update(Product product);
    boolean deleteById(UUID id);
    PageResult<Product> findAll(ProductCriteria criteria);
}
