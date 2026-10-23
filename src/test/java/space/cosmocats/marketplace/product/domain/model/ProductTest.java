package space.cosmocats.marketplace.product.domain.model;

import java.util.UUID;
import java.util.Currency;
import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.domain.exception.InsufficientStockException;
import space.cosmocats.marketplace.product.domain.exception.ProductPriceMustBePositiveException;
import space.cosmocats.marketplace.product.domain.exception.ProductNameMustContainSpaceWordException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ProductTest {

    private static final int INITIAL_STOCK = 5;
    private static final Currency CURRENCY = Currency.getInstance("USD");
    private static final UUID CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("Preserves the provided product ID")
    void shouldPreserveProvidedId() {
        // Given
        UUID expectedId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        var builder = validProductBuilder().id(expectedId);

        // When
        Product product = builder.build();

        // Then
        assertThat(product.getId()).isEqualTo(expectedId);
    }

    @Test
    @DisplayName("Generates a product ID when none is provided")
    void shouldGenerateIdWhenNotProvided() {
        var builder = validProductBuilder();

        Product product = builder.build();

        assertThat(product.getId()).isNotNull();
    }

    @ParameterizedTest(name = "[{index}] missing field = {0}")
    @DisplayName("Rejects building a product without a required field")
    @MethodSource("buildersWithMissingRequiredFields")
    void shouldRejectBuildingProductWithoutRequiredField(String fieldName, Product.ProductBuilder builder) {
        Throwable exception = catchThrowable(builder::build);

        assertThat(exception)
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessageContaining(fieldName);
    }

    @Test
    @DisplayName("Strips leading and trailing whitespace from the name")
    void shouldStripLeadingAndTrailingWhitespaceFromName() {
        var builder = validProductBuilder().name(" \tBlue Star\n ");

        Product product = builder.build();

        assertThat(product.getName()).isEqualTo("Blue Star");
    }

    @Test
    @DisplayName("Strips leading and trailing whitespace from the description")
    void shouldStripLeadingAndTrailingWhitespaceFromDescription() {
        var builder = validProductBuilder().description(" \tA portable cosmic lamp\n ");

        Product product = builder.build();

        assertThat(product.getDescription()).isEqualTo("A portable cosmic lamp");
    }

    @ParameterizedTest
    @DisplayName("Rejects an empty or whitespace-only name")
    @ValueSource(strings = {"", "   ", "\t\n"})
    void shouldRejectBlankName(String name) {
        var builder = validProductBuilder().name(name);

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @DisplayName("Rejects an empty or whitespace-only description")
    @ValueSource(strings = {"", "   ", "\t\n"})
    void shouldRejectBlankDescription(String description) {
        var builder = validProductBuilder().description(description);

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "[{index}] name = {0}")
    @DisplayName("Accepts a name containing a standalone cosmic word")
    @ValueSource(strings = {
            "STAR", "GALAXY", "COMET",
            "Blue star lamp", "Blue galaxy lamp", "Blue comet lamp",
            "sTaR-lamp", "gAlAxY-lamp", "cOmEt-lamp"
    })
    void shouldAcceptNameContainingCosmicWord(String name) {
        var builder = validProductBuilder().name(name);

        Product product = builder.build();

        assertThat(product.getName()).isEqualTo(name);
    }

    @ParameterizedTest(name = "[{index}] name = {0}")
    @DisplayName("Rejects a name without a standalone cosmic word")
    @ValueSource(strings = {"Ordinary lamp", "Starship engine", "GalaxyLamp", "Cometary dust"})
    void shouldRejectNameWithoutCosmicWord(String name) {
        var builder = validProductBuilder().name(name);

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isInstanceOf(ProductNameMustContainSpaceWordException.class);
    }

    @Test
    @DisplayName("Rejects a zero product price")
    void shouldRejectZeroPrice() {
        var builder = validProductBuilder().price(Money.zero(CURRENCY));

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isInstanceOf(ProductPriceMustBePositiveException.class);
    }

    @Test
    @DisplayName("Reduces stock by the requested quantity")
    void shouldRemoveRequestedQuantityFromStock() {
        Product product = validProductBuilder().build();
        Quantity requested = new Quantity(3);

        Product updatedProduct = product.removeStock(requested);

        assertThat(updatedProduct.getStock()).isEqualTo(new Quantity(2));
    }

    @Test
    @DisplayName("Allows removing all available stock")
    void shouldAllowRemovingAllStock() {
        Product product = validProductBuilder().build();
        Quantity requested = new Quantity(INITIAL_STOCK);

        Product updatedProduct = product.removeStock(requested);

        assertThat(updatedProduct.getStock()).isEqualTo(Quantity.ZERO);
    }

    @Test
    @DisplayName("Rejects a null quantity without changing stock")
    void shouldRejectRemovingNullQuantity() {
        Product product = validProductBuilder().build();

        Throwable exception = catchThrowable(() -> product.removeStock(null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
        assertThat(product.getStock()).isEqualTo(new Quantity(INITIAL_STOCK));
    }

    @Test
    @DisplayName("Rejects removing a zero quantity")
    void shouldRejectRemovingZeroQuantity() {
        Product product = validProductBuilder().build();

        Throwable exception = catchThrowable(() -> product.removeStock(Quantity.ZERO));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Rejects removing more than the available stock")
    void shouldRejectRemovingMoreThanAvailableStock() {
        Product product = validProductBuilder().build();
        Quantity requested = new Quantity(INITIAL_STOCK + 1);

        Throwable exception = catchThrowable(() -> product.removeStock(requested));

        assertThat(exception).isInstanceOf(InsufficientStockException.class);
        assertThat(product.getStock()).isEqualTo(new Quantity(INITIAL_STOCK));
    }

    @Test
    @DisplayName("Returns a new product with the same ID and preserves the original stock")
    void shouldReturnNewProductWithSameIdWithoutChangingOriginalStock() {
        Product originalProduct = validProductBuilder().build();
        Quantity requested = new Quantity(1);

        Product updatedProduct = originalProduct.removeStock(requested);

        assertThat(updatedProduct).isNotSameAs(originalProduct);
        assertThat(updatedProduct.getId()).isEqualTo(originalProduct.getId());
        assertThat(originalProduct.getStock()).isEqualTo(new Quantity(INITIAL_STOCK));
    }

    private static Stream<Arguments> buildersWithMissingRequiredFields() {
        String name = "Blue Star";
        String description = "A portable cosmic lamp";
        Money price = new Money(CURRENCY, new BigDecimal("12.50"));
        Quantity stock = new Quantity(INITIAL_STOCK);

        return Stream.of(
                Arguments.of("name", Product.builder()
                        .description(description)
                        .price(price)
                        .categoryId(CATEGORY_ID)
                        .stock(stock)),
                Arguments.of("description", Product.builder()
                        .name(name)
                        .price(price)
                        .categoryId(CATEGORY_ID)
                        .stock(stock)),
                Arguments.of("price", Product.builder()
                        .name(name)
                        .description(description)
                        .categoryId(CATEGORY_ID)
                        .stock(stock)),
                Arguments.of("categoryId", Product.builder()
                        .name(name)
                        .description(description)
                        .price(price)
                        .stock(stock)),
                Arguments.of("stock", Product.builder()
                        .name(name)
                        .description(description)
                        .price(price)
                        .categoryId(CATEGORY_ID))
        );
    }

    private static Product.ProductBuilder validProductBuilder() {
        return Product.builder()
                .name("Blue Star")
                .description("A portable cosmic lamp")
                .price(new Money(CURRENCY, new BigDecimal("12.50")))
                .categoryId(CATEGORY_ID)
                .stock(new Quantity(INITIAL_STOCK));
    }
}
