package space.cosmocats.marketplace.product.application.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import space.cosmocats.marketplace.product.application.exception.CategoryNotFoundException;
import space.cosmocats.marketplace.product.application.exception.ProductConflictException;
import space.cosmocats.marketplace.product.domain.repository.CategoryRepository;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.common.pagination.PageResult;
import org.springframework.transaction.annotation.Transactional;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.application.service.ProductService;
import space.cosmocats.marketplace.product.domain.repository.ProductRepository;
import space.cosmocats.marketplace.product.application.exception.ProductNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

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
    @Transactional
    public Product createProduct(Product product) {
        if (productRepository.findById(product.getId()).isPresent()) {
            throw new ProductConflictException(product.getId());
        }
        validateCategoryAndName(product);
        try {
            return productRepository.save(product);
        } catch (DataIntegrityViolationException ex) {
            throw new ProductConflictException(product.getId(), ex);
        }
    }

    @Override
    @Transactional
    public Product updateProduct(Product product) {
        if (productRepository.findById(product.getId()).isEmpty()) {
            throw new ProductNotFoundException(product.getId());
        }
        validateCategoryAndName(product);
        try {
            return productRepository.update(product)
                    .orElseThrow(() -> new ProductNotFoundException(product.getId()));
        } catch (DataIntegrityViolationException ex) {
            throw new ProductConflictException(product.getId(), ex);
        }
    }

    @Override
    @Transactional
    public void deleteProduct(UUID id) {
        try {
            if (!productRepository.deleteById(id)) {
                throw new ProductNotFoundException(id);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new ProductConflictException(id, ex);
        }
    }

    private void validateCategoryAndName(Product product) {
        categoryRepository.findById(product.getCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(product.getCategoryId()));
        if (productRepository.existsByCategoryIdAndNameAndIdNot(product.getCategoryId(), product.getName(), product.getId())) {
            throw new ProductConflictException(product.getId());
        }
    }
}
