package space.cosmocats.marketplace.order.domain.model.value;

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

import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OrderItemTest {

    private static final String PRODUCT_NAME = "Blue Star";
    private static final Quantity QUANTITY = new Quantity(2);
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Money UNIT_PRICE = new Money(Currency.getInstance("USD"), BigDecimal.TEN);

    @Test
    @DisplayName("Creates an order item with a normalized name and preserves its values")
    void shouldCreateItemWithNormalizedName() {
        // Given
        String productName = "\u2003 Blue Star \t\n";

        // When
        OrderItem item = new OrderItem(PRODUCT_ID, productName, UNIT_PRICE, QUANTITY);

        // Then
        assertThat(item.productId()).isEqualTo(PRODUCT_ID);
        assertThat(item.productName()).isEqualTo(PRODUCT_NAME);
        assertThat(item.unitPrice()).isEqualTo(UNIT_PRICE);
        assertThat(item.quantity()).isEqualTo(QUANTITY);
    }

    @ParameterizedTest(name = "[{index}] null field = {0}")
    @DisplayName("Rejects null values in required fields")
    @MethodSource("itemsWithNullRequiredFields")
    void shouldRejectNullRequiredField(
            String fieldName, UUID productId, String productName, Money unitPrice, Quantity quantity
    ) {
        Throwable exception = catchThrowable(() -> new OrderItem(productId, productName, unitPrice, quantity));

        assertThat(exception)
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessageContaining(fieldName);
    }

    @ParameterizedTest(name = "[{index}] name = {0}")
    @DisplayName("Rejects an empty or whitespace-only product name")
    @ValueSource(strings = {"", " \t\n\u2003 "})
    void shouldRejectBlankProductName(String productName) {
        Throwable exception = catchThrowable(() -> new OrderItem(PRODUCT_ID, productName, UNIT_PRICE, QUANTITY));

        assertThat(exception)
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order item product name must not be blank");
    }

    @Test
    @DisplayName("Rejects a zero unit price")
    void shouldRejectZeroUnitPrice() {
        Money zeroPrice = Money.zero(UNIT_PRICE.currency());

        Throwable exception = catchThrowable(() -> new OrderItem(PRODUCT_ID, PRODUCT_NAME, zeroPrice, QUANTITY));

        assertThat(exception)
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order item unit price must be greater than zero");
    }

    @Test
    @DisplayName("Rejects a zero quantity")
    void shouldRejectZeroQuantity() {
        Throwable exception = catchThrowable(() -> new OrderItem(PRODUCT_ID, PRODUCT_NAME, UNIT_PRICE, Quantity.ZERO));

        assertThat(exception)
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order item quantity must be greater than zero");
    }

    @Test
    @DisplayName("Creates an item from a product using the requested quantity")
    void shouldCreateItemFromProduct() {
        Product product = Product.builder()
                .id(PRODUCT_ID)
                .name(PRODUCT_NAME)
                .description("A portable cosmic lamp")
                .price(UNIT_PRICE)
                .categoryId(UUID.fromString("00000000-0000-0000-0000-000000000002"))
                .stock(new Quantity(7))
                .build();

        OrderItem item = OrderItem.of(product, QUANTITY);

        assertThat(item.productId()).isEqualTo(product.getId());
        assertThat(item.productName()).isEqualTo(product.getName());
        assertThat(item.unitPrice()).isEqualTo(product.getPrice());
        assertThat(item.quantity()).isEqualTo(QUANTITY);
    }

    @Test
    @DisplayName("Calculates the subtotal as unit price multiplied by quantity")
    void shouldCalculateSubtotal() {
        Money unitPrice = new Money(UNIT_PRICE.currency(), new BigDecimal("12.50"));
        OrderItem item = new OrderItem(PRODUCT_ID, PRODUCT_NAME, unitPrice, new Quantity(3));

        Money subtotal = item.subtotal();

        assertThat(subtotal).isEqualTo(new Money(UNIT_PRICE.currency(), new BigDecimal("37.50")));
    }

    private static Stream<Arguments> itemsWithNullRequiredFields() {
        return Stream.of(
                Arguments.of("productId", null, PRODUCT_NAME, UNIT_PRICE, QUANTITY),
                Arguments.of("productName", PRODUCT_ID, null, UNIT_PRICE, QUANTITY),
                Arguments.of("unitPrice", PRODUCT_ID, PRODUCT_NAME, null, QUANTITY),
                Arguments.of("quantity", PRODUCT_ID, PRODUCT_NAME, UNIT_PRICE, null)
        );
    }
}
