package space.cosmocats.marketplace.integration;

import java.util.UUID;
import java.util.List;
import java.util.Currency;
import java.util.Optional;
import java.math.BigDecimal;

import java.util.stream.Stream;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import space.cosmocats.marketplace.common.pagination.PageResult;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.domain.repository.ProductSort;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.persistence.ProductPersistenceMapperImpl;

import space.cosmocats.marketplace.product.persistence.repository.JpaProductRepository;
import space.cosmocats.marketplace.product.persistence.repository.ProductRepositoryAdapter;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ProductRepositoryAdapter.class, ProductPersistenceMapperImpl.class})
class ProductRepositoryAdapterIntegrationTest {

    private static final UUID STAR_LAMP_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID GALAXY_BOWL_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID COMET_TOY_ID = UUID.fromString("30000000-0000-4000-8000-000000000003");
    private static final UUID STAR_BLANKET_ID = UUID.fromString("30000000-0000-4000-8000-000000000004");
    private static final UUID NEW_PRODUCT_ID = UUID.fromString("30000000-0000-4000-8000-000000000005");
    private static final UUID FIRST_CATEGORY_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final UUID SECOND_CATEGORY_ID = UUID.fromString("40000000-0000-4000-8000-000000000002");

    @Autowired
    private ProductRepositoryAdapter productRepositoryAdapter;

    @Autowired
    private JpaProductRepository jpaProductRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Finds an existing product by ID and maps all fields")
    void shouldFindExistingProductById() {
        // Given
        UUID productId = STAR_LAMP_ID;

        // When
        Optional<Product> result = productRepositoryAdapter.findById(productId);

        // Then
        assertThat(result).hasValueSatisfying(product -> {
            assertThat(product.getId()).isEqualTo(productId);
            assertThat(product.getName()).isEqualTo("Star Lamp");
            assertThat(product.getDescription())
                    .isEqualTo("A compact star lamp for repository integration tests.");
            assertThat(product.getPrice().amount()).isEqualByComparingTo("10.00");
            assertThat(product.getPrice().currency().getCurrencyCode()).isEqualTo("USD");
            assertThat(product.getStock().value()).isEqualTo(5);
            assertThat(product.getCategoryId()).isEqualTo(FIRST_CATEGORY_ID);
        });
    }

    @Test
    @DisplayName("Returns an empty optional for an unknown product ID")
    void shouldReturnEmptyWhenProductDoesNotExist() {
        Optional<Product> result = productRepositoryAdapter.findById(NEW_PRODUCT_ID);
        assertThat(result).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("sortingScenarios")
    @DisplayName("Sorts products by price and breaks ties by ascending ID")
    void shouldSortProductsByPriceAndId(ProductSort sort, List<UUID> expectedIds) {
        ProductCriteria criteria = new ProductCriteria(0, 10, sort);

        PageResult<Product> result = productRepositoryAdapter.findAll(criteria);

        assertThat(result.content()).extracting(Product::getId).containsExactlyElementsOf(expectedIds);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(4);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.hasContent()).isTrue();
    }


    @ParameterizedTest(name = "page={0}, size={1}")
    @MethodSource("paginationScenarios")
    @DisplayName("Returns the requested page with correct pagination metadata")
    void shouldReturnRequestedPage(int page, int size, List<UUID> expectedIds) {
        ProductCriteria criteria = new ProductCriteria(page, size, ProductSort.PRICE_ASC);

        PageResult<Product> result = productRepositoryAdapter.findAll(criteria);

        assertThat(result.content()).extracting(Product::getId).containsExactlyElementsOf(expectedIds);
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(size);
        assertThat(result.totalElements()).isEqualTo(4);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasContent()).isTrue();
    }

    @Test
    @DisplayName("Returns an empty page when the requested page is out of range")
    void shouldReturnEmptyPageWhenPageIsOutOfRange() {
        ProductCriteria criteria = new ProductCriteria(2, 2, ProductSort.PRICE_ASC);

        PageResult<Product> result = productRepositoryAdapter.findAll(criteria);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(4);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.hasContent()).isFalse();
    }

    @Test
    @DisplayName("Returns an empty page when the product table is empty")
    void shouldReturnEmptyPageWhenThereAreNoProducts() {
        jpaProductRepository.deleteAllInBatch();
        entityManager.clear();
        ProductCriteria criteria = new ProductCriteria(0, 10, ProductSort.PRICE_ASC);

        PageResult<Product> result = productRepositoryAdapter.findAll(criteria);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.hasContent()).isFalse();
    }

    @Test
    @DisplayName("Saves a new product and persists all fields")
    void shouldSaveNewProduct() {
        Product product = validProductBuilder().build();
        long initialCount = jpaProductRepository.count();

        Product result = productRepositoryAdapter.save(product);
        flushAndClear();

        assertThat(result).usingRecursiveComparison().isEqualTo(product);
        assertThat(productRepositoryAdapter.findById(product.getId()))
                .hasValueSatisfying(persisted ->
                        assertThat(persisted).usingRecursiveComparison().isEqualTo(product));
        assertThat(jpaProductRepository.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("Updates all product fields without changing its ID or inserting a row")
    void shouldUpdateExistingProduct() {
        Product product = validProductBuilder().id(STAR_LAMP_ID).build();
        long initialCount = jpaProductRepository.count();

        Optional<Product> result = productRepositoryAdapter.update(product);
        flushAndClear();

        assertThat(result).hasValueSatisfying(updated ->
                assertThat(updated).usingRecursiveComparison().isEqualTo(product));
        assertThat(productRepositoryAdapter.findById(product.getId()))
                .hasValueSatisfying(persisted ->
                        assertThat(persisted).usingRecursiveComparison().isEqualTo(product));
        assertThat(jpaProductRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("Returns an empty optional without inserting an unknown product")
    void shouldNotCreateProductWhenUpdatingUnknownId() {
        Product product = validProductBuilder().build();
        long initialCount = jpaProductRepository.count();

        Optional<Product> result = productRepositoryAdapter.update(product);
        flushAndClear();

        assertThat(result).isEmpty();
        assertThat(productRepositoryAdapter.findById(product.getId())).isEmpty();
        assertThat(jpaProductRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("Deletes an existing product and returns true")
    void shouldDeleteExistingProduct() {
        UUID productId = STAR_LAMP_ID;
        long initialCount = jpaProductRepository.count();

        boolean result = productRepositoryAdapter.deleteById(productId);
        flushAndClear();

        assertThat(result).isTrue();
        assertThat(productRepositoryAdapter.findById(productId)).isEmpty();
        assertThat(jpaProductRepository.count()).isEqualTo(initialCount - 1);
    }

    @Test
    @DisplayName("Returns false without deleting rows for an unknown product ID")
    void shouldReturnFalseWhenDeletingUnknownProduct() {
        long initialCount = jpaProductRepository.count();

        boolean result = productRepositoryAdapter.deleteById(NEW_PRODUCT_ID);
        flushAndClear();

        assertThat(result).isFalse();
        assertThat(jpaProductRepository.count()).isEqualTo(initialCount);
    }

    private static Stream<Arguments> sortingScenarios() {
        return Stream.of(
                Arguments.of(
                        ProductSort.PRICE_ASC,
                        List.of(STAR_LAMP_ID, COMET_TOY_ID, STAR_BLANKET_ID, GALAXY_BOWL_ID)
                ),
                Arguments.of(
                        ProductSort.PRICE_DESC,
                        List.of(GALAXY_BOWL_ID, COMET_TOY_ID, STAR_BLANKET_ID, STAR_LAMP_ID)
                )
        );
    }

    private static Stream<Arguments> paginationScenarios() {
        return Stream.of(
                Arguments.of(0, 2, List.of(STAR_LAMP_ID, COMET_TOY_ID)),
                Arguments.of(1, 2, List.of(STAR_BLANKET_ID, GALAXY_BOWL_ID)),
                Arguments.of(1, 3, List.of(GALAXY_BOWL_ID))
        );
    }

    private Product.ProductBuilder validProductBuilder() {
        return Product.builder()
                .id(NEW_PRODUCT_ID)
                .name("Comet Carrier")
                .description("A spacious comet carrier for repository integration tests.")
                .price(new Money(Currency.getInstance("EUR"), new BigDecimal("47.95")))
                .stock(new Quantity(7))
                .categoryId(SECOND_CATEGORY_ID);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
