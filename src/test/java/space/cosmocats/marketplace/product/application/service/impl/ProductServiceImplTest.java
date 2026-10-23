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
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.domain.repository.ProductSort;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.domain.repository.ProductRepository;
import space.cosmocats.marketplace.product.application.exception.ProductNotFoundException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("Returns the repository page for the given product criteria")
    void shouldReturnProductsPageForGivenCriteria() {
        // Given
        ProductCriteria criteria = new ProductCriteria(0, 10, ProductSort.PRICE_ASC);
        PageResult<Product> expectedPage = PageResult.<Product>builder()
                .content(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .build();
        when(productRepository.findAll(criteria)).thenReturn(expectedPage);

        // When
        PageResult<Product> actualPage = productService.getAllProducts(criteria);

        // Then
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
        when(productRepository.update(product)).thenReturn(Optional.of(updatedProduct));

        Product actualProduct = productService.updateProduct(product);

        assertThat(actualProduct).isSameAs(updatedProduct);
        verify(productRepository).update(product);
    }

    @Test
    @DisplayName("Throws ProductNotFoundException when the product to update is missing")
    void shouldThrowWhenProductIsNotFoundForUpdate() {
        Product product = validProductBuilder().build();
        when(productRepository.update(product)).thenReturn(Optional.empty());

        Throwable exception = catchThrowable(() -> productService.updateProduct(product));

        assertThat(exception).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository).update(product);
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

    private static Product.ProductBuilder validProductBuilder() {
        return Product.builder()
                .name("Blue Star")
                .description("A portable cosmic lamp")
                .price(new Money(Currency.getInstance("USD"), new BigDecimal("12.50")))
                .categoryId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .stock(new Quantity(5));
    }
}
