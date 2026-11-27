package space.cosmocats.marketplace.integration;

import java.util.UUID;
import java.util.Currency;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.domain.repository.ProductSort;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.application.service.ProductService;
import space.cosmocats.marketplace.product.application.service.impl.ProductServiceImpl;
import space.cosmocats.marketplace.product.application.exception.CategoryNotFoundException;
import space.cosmocats.marketplace.product.application.exception.ProductConflictException;
import space.cosmocats.marketplace.product.application.exception.ProductNotFoundException;
import space.cosmocats.marketplace.product.persistence.repository.ProductRepositoryAdapter;
import space.cosmocats.marketplace.product.persistence.mapper.ProductPersistenceMapperImpl;
import space.cosmocats.marketplace.product.persistence.mapper.CategoryPersistenceMapperImpl;
import space.cosmocats.marketplace.product.persistence.repository.CategoryRepositoryAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        ProductServiceImpl.class,
        ProductRepositoryAdapter.class,
        CategoryRepositoryAdapter.class,
        ProductPersistenceMapperImpl.class,
        CategoryPersistenceMapperImpl.class
})
class ProductServiceIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID CATEGORY_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("40000000-0000-4000-8000-000000000002");
    private static final UUID MISSING_CATEGORY_ID = UUID.fromString("40000000-0000-4000-8000-000000000099");
    private static final Currency USD = Currency.getInstance("USD");

    @Autowired
    private ProductService productService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Creates a product in an existing category and persists all fields")
    void shouldCreateProductInExistingCategory() {
        Product product = validProductBuilder().build();

        Product created = productService.createProduct(product);
        entityManager.clear();
        Product persisted = productService.getProductById(product.getId());

        assertThat(created).usingRecursiveComparison().isEqualTo(product);
        assertThat(persisted).usingRecursiveComparison().isEqualTo(product);
    }

    @Test
    @DisplayName("Updates a product name and category without changing its ID")
    void shouldUpdateProductAndCategory() {
        Product existing = productService.createProduct(validProductBuilder().build());
        Product changed = existing.withName("Galaxy Renamed").withCategoryId(OTHER_CATEGORY_ID);

        Product updated = productService.updateProduct(changed);
        entityManager.clear();
        Product persisted = productService.getProductById(existing.getId());

        assertThat(updated).usingRecursiveComparison().isEqualTo(changed);
        assertThat(persisted).usingRecursiveComparison().isEqualTo(changed);
    }

    @Test
    @DisplayName("Includes a newly created product in the requested page")
    void shouldIncludeCreatedProductInPage() {
        Product created = productService.createProduct(validProductBuilder().build());
        ProductCriteria criteria = new ProductCriteria(0, 20, ProductSort.PRICE_ASC);
        entityManager.clear();

        var page = productService.getAllProducts(criteria);

        assertThat(page.content()).extracting(Product::getId).contains(created.getId());
    }

    @Test
    @DisplayName("Deletes a product that is not referenced by an order")
    void shouldDeleteUnusedProduct() {
        Product created = productService.createProduct(validProductBuilder().build());

        productService.deleteProduct(created.getId());
        entityManager.clear();
        Throwable exception = catchThrowable(() -> productService.getProductById(created.getId()));

        assertThat(exception).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("Rejects creating a product in a missing category")
    void shouldRejectMissingCategoryOnCreate() {
        Product product = validProductBuilder().categoryId(MISSING_CATEGORY_ID).build();

        Throwable exception = catchThrowable(() -> productService.createProduct(product));

        assertThat(exception).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("Rejects moving a product to a missing category and preserves its current category")
    void shouldRejectMissingCategoryOnUpdate() {
        Product existing = productService.createProduct(validProductBuilder().build());
        Product changed = existing.withCategoryId(MISSING_CATEGORY_ID);

        Throwable exception = catchThrowable(() -> productService.updateProduct(changed));
        entityManager.clear();

        assertThat(exception).isInstanceOf(CategoryNotFoundException.class);
        assertThat(productService.getProductById(existing.getId()).getCategoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    @DisplayName("Rejects a duplicate product ID without overwriting the existing product")
    void shouldRejectDuplicateIdWithoutOverwritingProduct() {
        Product existing = productService.createProduct(validProductBuilder().name("Star Original").build());
        Product replacement = existing.withName("Star Replacement");

        Throwable exception = catchThrowable(() -> productService.createProduct(replacement));

        assertThat(exception).isInstanceOf(ProductConflictException.class);
        assertThat(productService.getProductById(existing.getId()).getName()).isEqualTo("Star Original");
    }

    @Test
    @DisplayName("Rejects a duplicate product name in the same category after normalization")
    void shouldRejectDuplicateNormalizedNameInCategory() {
        productService.createProduct(validProductBuilder().name("Star Unique").build());
        Product duplicate = validProductBuilder().name("  Star Unique  ").build();

        Throwable exception = catchThrowable(() -> productService.createProduct(duplicate));

        assertThat(exception).isInstanceOf(ProductConflictException.class);
    }

    @Test
    @DisplayName("Allows the same product name in different categories")
    void shouldAllowSameNameInDifferentCategories() {
        productService.createProduct(validProductBuilder().name("Star Unique").build());
        Product product = validProductBuilder().name("Star Unique").categoryId(OTHER_CATEGORY_ID).build();

        Product created = productService.createProduct(product);
        entityManager.clear();

        assertThat(productService.getProductById(created.getId())).usingRecursiveComparison().isEqualTo(product);
    }

    @Test
    @DisplayName("Rejects moving a product to a category containing the same name")
    void shouldRejectDuplicateNameOnCategoryChange() {
        productService.createProduct(validProductBuilder().name("Star Unique").build());
        Product otherCategory = productService.createProduct(
                validProductBuilder().name("Star Unique").categoryId(OTHER_CATEGORY_ID).build()
        );
        Product changed = otherCategory.withCategoryId(CATEGORY_ID);

        Throwable exception = catchThrowable(() -> productService.updateProduct(changed));

        assertThat(exception).isInstanceOf(ProductConflictException.class);
    }

    @Test
    @DisplayName("Allows updating a product without changing its own name")
    void shouldAllowUnchangedProductNameOnUpdate() {
        Product existing = productService.createProduct(validProductBuilder().build());
        Product changed = existing.withDescription("Updated product description");

        Product updated = productService.updateProduct(changed);
        entityManager.clear();

        assertThat(updated).usingRecursiveComparison().isEqualTo(changed);
        assertThat(productService.getProductById(existing.getId())).usingRecursiveComparison().isEqualTo(changed);
    }

    @Test
    @DisplayName("Rejects renaming a product to an existing name in its category")
    void shouldRejectRenamingToExistingName() {
        productService.createProduct(validProductBuilder().name("Star First").build());
        Product second = productService.createProduct(validProductBuilder().name("Star Second").build());
        Product renamed = second.withName("Star First");

        Throwable exception = catchThrowable(() -> productService.updateProduct(renamed));

        assertThat(exception).isInstanceOf(ProductConflictException.class);
    }

    @Test
    @DisplayName("Reports a missing product before validating its category on update")
    void shouldReportMissingProductBeforeItsCategory() {
        Product missing = validProductBuilder().categoryId(MISSING_CATEGORY_ID).build();

        Throwable exception = catchThrowable(() -> productService.updateProduct(missing));

        assertThat(exception).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("Throws ProductNotFoundException when deleting an unknown product")
    void shouldThrowWhenProductIsNotFoundForDeletion() {
        Product missing = validProductBuilder().build();

        Throwable exception = catchThrowable(() -> productService.deleteProduct(missing.getId()));

        assertThat(exception).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    @DisplayName("Translates deletion of an ordered product into a conflict")
    void shouldRejectDeletingProductUsedByOrder() {
        Product product = productService.createProduct(validProductBuilder().build());
        UUID orderId = UUID.randomUUID();

        jdbcTemplate.update("""
                INSERT INTO orders (id, buyer_id, created_at, status, currency)
                VALUES (?, ?, current_timestamp, 'CREATED', 'USD')
                """, orderId, UUID.randomUUID());

        jdbcTemplate.update("""
                INSERT INTO order_items (order_id, position, product_id, product_name, unit_price_amount, quantity)
                VALUES (?, 0, ?, ?, 1, 1)
                """, orderId, product.getId(), product.getName());

        Throwable exception = catchThrowable(() -> productService.deleteProduct(product.getId()));

        assertThat(exception).isInstanceOf(ProductConflictException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    private static Product.ProductBuilder validProductBuilder() {
        return Product.builder()
                .name("Star New")
                .description("Product service integration test item")
                .categoryId(CATEGORY_ID)
                .price(new Money(USD, new BigDecimal("1.2345")))
                .stock(new Quantity(3));
    }
}
