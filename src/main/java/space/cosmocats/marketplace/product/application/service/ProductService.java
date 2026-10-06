package space.cosmocats.marketplace.product.application.service;

import java.util.UUID;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.common.pagination.PageResult;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;

public interface ProductService {
    void deleteProduct(UUID id);
    Product getProductById(UUID id);
    Product createProduct(Product product);
    Product updateProduct(Product product);
    PageResult<Product> getAllProducts(ProductCriteria criteria);
}
