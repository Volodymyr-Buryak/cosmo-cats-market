package space.cosmocats.marketplace.product.persistence.repository;

import java.util.UUID;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.common.pagination.PageResult;
import space.cosmocats.marketplace.product.domain.repository.ProductSort;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.domain.repository.ProductRepository;
import space.cosmocats.marketplace.product.persistence.ProductPersistenceMapper;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final JpaProductRepository jpaProductRepository;
    private final ProductPersistenceMapper productPersistenceMapper;

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaProductRepository.findById(id)
                .map(productPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Product> findAll(ProductCriteria criteria) {
        return productPersistenceMapper.toPageResult(
                jpaProductRepository.findAll(
                        PageRequest.of(criteria.page(), criteria.size(), toSort(criteria.sort()))
                )
        );
    }

    @Override
    public Product save(Product product) {
        return productPersistenceMapper.toDomain(
                jpaProductRepository.save(productPersistenceMapper.toEntity(product))
        );
    }

    @Override
    public Optional<Product> update(Product product) {
        return jpaProductRepository.findById(product.getId())
                .map(entity -> {
                    productPersistenceMapper.updateEntity(product, entity);
                    return productPersistenceMapper.toDomain(jpaProductRepository.save(entity));
                });
    }

    @Override
    public boolean deleteById(UUID id) {
        return jpaProductRepository.findById(id)
                .map(entity -> {
                    jpaProductRepository.delete(entity);
                    return true;
                })
                .orElse(false);
    }

    private Sort toSort(ProductSort sort) {
        return switch (sort) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price")
                    .and(Sort.by(Sort.Direction.ASC, "id"));
        };
    }
}
