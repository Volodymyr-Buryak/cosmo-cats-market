package space.cosmocats.marketplace.cart.domain.model.value;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.cart.domain.exception.CartItemQuantityMustBePositiveException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CartItemTest {

    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @ParameterizedTest(name = "[{index}] quantity = {0}")
    @DisplayName("Creates an item with a positive quantity and preserves its values")
    @ValueSource(ints = {1, Integer.MAX_VALUE})
    void shouldCreateItemWithPositiveQuantity(int value) {
        // Given
        Quantity quantity = new Quantity(value);

        // When
        CartItem item = new CartItem(PRODUCT_ID, quantity);

        // Then
        assertThat(item.productId()).isEqualTo(PRODUCT_ID);
        assertThat(item.quantity()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("Rejects creating an item with zero quantity")
    void shouldRejectZeroQuantity() {
        Throwable exception = catchThrowable(() -> new CartItem(PRODUCT_ID, Quantity.ZERO));

        assertThat(exception).isInstanceOf(CartItemQuantityMustBePositiveException.class);
    }

    @Test
    @DisplayName("Rejects creating an item with a null product ID")
    void shouldRejectNullProductId() {
        Quantity quantity = new Quantity(1);

        Throwable exception = catchThrowable(() -> new CartItem(null, quantity));

        assertThat(exception)
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessageContaining("productId");
    }

    @Test
    @DisplayName("Rejects creating an item with a null quantity")
    void shouldRejectNullQuantity() {
        Throwable exception = catchThrowable(() -> new CartItem(PRODUCT_ID, null));

        assertThat(exception)
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessageContaining("quantity");
    }
}
