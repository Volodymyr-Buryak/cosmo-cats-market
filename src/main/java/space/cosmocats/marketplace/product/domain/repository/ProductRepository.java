package space.cosmocats.marketplace.product.domain.repository;

import java.util.UUID;
import java.util.Optional;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.common.pagination.PageResult;

public interface ProductRepository {
    boolean deleteById(UUID id);
    Product save(Product product);
    Optional<Product> findById(UUID id);
    Optional<Product> update(Product product);
    PageResult<Product> findAll(ProductCriteria criteria);
}
