package space.cosmocats.marketplace.product.application.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.Currency;
import java.util.Optional;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import space.cosmocats.marketplace.common.pagination.PageResult;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.product.domain.model.Category;
import space.cosmocats.marketplace.product.domain.repository.CategoryRepository;
import space.cosmocats.marketplace.product.application.exception.CategoryNotFoundException;
import space.cosmocats.marketplace.product.application.exception.ProductConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.domain.repository.ProductSort;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.domain.repository.ProductRepository;
import space.cosmocats.marketplace.product.application.exception.ProductNotFoundException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("Returns the repository page for the given product criteria")
    void shouldReturnProductsPageForGivenCriteria() {
        ProductCriteria criteria = new ProductCriteria(0, 10, ProductSort.PRICE_ASC);
        PageResult<Product> expectedPage = PageResult.<Product>builder()
                .content(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();
        when(productRepository.findAll(criteria)).thenReturn(expectedPage);

        PageResult<Product> actualPage = productService.getAllProducts(criteria);

        assertThat(actualPage).isSameAs(expectedPage);
        verify(productRepository).findAll(criteria);
    }

    @Test
    @DisplayName("Returns the product when it is found by ID")
    void shouldReturnProductWhenFoundById() {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Product expectedProduct = validProductBuilder()
                .id(productId)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(expectedProduct));

        Product actualProduct = productService.getProductById(productId);

        assertThat(actualProduct).isSameAs(expectedProduct);
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("Throws ProductNotFoundException when no product is found by ID")
    void shouldThrowWhenProductIsNotFoundById() {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        Throwable exception = catchThrowable(() -> productService.getProductById(productId));

        assertThat(exception).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository).findById(productId);
    }

    @Test
    @DisplayName("Returns the product saved by the repository")
    void shouldReturnSavedProduct() {
        Product product = validProductBuilder().build();
        Product savedProduct = validProductBuilder().id(product.getId()).build();
        stubExistingCategory(product);
        when(productRepository.save(product)).thenReturn(savedProduct);

        Product actualProduct = productService.createProduct(product);

        assertThat(actualProduct).isSameAs(savedProduct);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Returns the product updated by the repository")
    void shouldReturnUpdatedProduct() {
        Product product = validProductBuilder().build();
        Product updatedProduct = validProductBuilder().id(product.getId()).build();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        stubExistingCategory(product);
        when(productRepository.update(product)).thenReturn(Optional.of(updatedProduct));

        Product actualProduct = productService.updateProduct(product);

        assertThat(actualProduct).isSameAs(updatedProduct);
        verify(productRepository).update(product);
    }

    @Test
    @DisplayName("Throws ProductNotFoundException when the product to update is missing")
    void shouldThrowWhenProductIsNotFoundForUpdate() {
        Product product = validProductBuilder().build();
        when(productRepository.findById(product.getId())).thenReturn(Optional.empty());

        Throwable exception = catchThrowable(() -> productService.updateProduct(product));

        assertThat(exception).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).update(product);
    }

    @Test
    @DisplayName("Deletes the product when the repository confirms deletion")
    void shouldDeleteProductWhenRepositoryConfirmsDeletion() {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(productRepository.deleteById(productId)).thenReturn(true);

        productService.deleteProduct(productId);

        verify(productRepository).deleteById(productId);
    }

    @Test
    @DisplayName("Throws ProductNotFoundException when the product to delete is missing")
    void shouldThrowWhenProductIsNotFoundForDeletion() {
        UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(productRepository.deleteById(productId)).thenReturn(false);

        Throwable exception = catchThrowable(() -> productService.deleteProduct(productId));

        assertThat(exception).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository).deleteById(productId);
    }

    @Test
    @DisplayName("Rejects creating a product in a missing category")
    void shouldRejectMissingCategoryOnCreate() {
        Product product = validProductBuilder().build();

        Throwable exception = catchThrowable(() -> productService.createProduct(product));

        assertThat(exception).isInstanceOf(CategoryNotFoundException.class);
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Rejects creating a product with an existing ID")
    void shouldRejectExistingProductId() {
        Product product = validProductBuilder().build();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        Throwable exception = catchThrowable(() -> productService.createProduct(product));

        assertThat(exception).isInstanceOf(ProductConflictException.class);
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Rejects creating a product with an existing name in its category")
    void shouldRejectDuplicateNameInCategory() {
        Product product = validProductBuilder().build();
        stubExistingCategory(product);
        when(productRepository.existsByCategoryIdAndNameAndIdNot(product.getCategoryId(), product.getName(), product.getId()))
                .thenReturn(true);

        Throwable exception = catchThrowable(() -> productService.createProduct(product));

        assertThat(exception).isInstanceOf(ProductConflictException.class);
        verify(productRepository, never()).save(product);
    }

    @Test
    @DisplayName("Translates a database constraint violation when creating a product")
    void shouldTranslateConstraintViolationOnCreate() {
        Product product = validProductBuilder().build();
        stubExistingCategory(product);
        var cause = new DataIntegrityViolationException("Product constraint violation");
        when(productRepository.save(product)).thenThrow(cause);

        Throwable exception = catchThrowable(() -> productService.createProduct(product));

        assertThat(exception).isInstanceOf(ProductConflictException.class).hasCause(cause);
    }

    @Test
    @DisplayName("Translates a database constraint violation when updating a product")
    void shouldTranslateConstraintViolationOnUpdate() {
        Product product = validProductBuilder().build();
        stubExistingCategory(product);
        var cause = new DataIntegrityViolationException("Product constraint violation");
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.update(product)).thenThrow(cause);

        Throwable exception = catchThrowable(() -> productService.updateProduct(product));

        assertThat(exception).isInstanceOf(ProductConflictException.class).hasCause(cause);
    }

    @Test
    @DisplayName("Translates a database constraint violation when deleting a product")
    void shouldTranslateConstraintViolationOnDelete() {
        Product product = validProductBuilder().build();
        var cause = new DataIntegrityViolationException("Product constraint violation");
        when(productRepository.deleteById(product.getId())).thenThrow(cause);

        Throwable exception = catchThrowable(() -> productService.deleteProduct(product.getId()));

        assertThat(exception).isInstanceOf(ProductConflictException.class).hasCause(cause);
    }

    private void stubExistingCategory(Product product) {
        Category category = Category.builder()
                .id(product.getCategoryId())
                .name("Category")
                .description("Product category")
                .build();
        when(categoryRepository.findById(product.getCategoryId())).thenReturn(Optional.of(category));
    }

    private static Product.ProductBuilder validProductBuilder() {
        return Product.builder()
                .name("Blue Star")
                .description("A portable cosmic lamp")
                .price(new Money(Currency.getInstance("USD"), new BigDecimal("12.50")))
                .categoryId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .stock(new Quantity(5));
    }
}
