package space.cosmocats.marketplace.product.application.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.common.pagination.PageResult;
import org.springframework.transaction.annotation.Transactional;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.application.service.ProductService;
import space.cosmocats.marketplace.product.domain.repository.ProductRepository;
import space.cosmocats.marketplace.product.application.exception.ProductNotFoundException;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public PageResult<Product> getAllProducts(ProductCriteria criteria) {
        return productRepository.findAll(criteria);
    }

    @Override
    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Override
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(Product product) {
        return productRepository.update(product)
                .orElseThrow(() -> new ProductNotFoundException(product.getId()));
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.deleteById(id)) {
            throw new ProductNotFoundException(id);
        }
    }

}
