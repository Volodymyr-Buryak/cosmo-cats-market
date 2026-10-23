package space.cosmocats.marketplace.cart.domain.model;

import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import space.cosmocats.marketplace.cart.domain.model.value.CartItem;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.cart.domain.exception.CartItemNotFoundException;
import space.cosmocats.marketplace.cart.domain.exception.CartItemQuantityMustBePositiveException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CartTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    @DisplayName("Adds a new product to an empty cart with the requested quantity")
    void shouldAddNewProductToEmptyCart() {
        // Given
        Cart cart = Cart.create(USER_ID);
        Quantity quantity = new Quantity(2);

        // When
        cart.addItem(PRODUCT_ID, quantity);

        // Then
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, quantity));
    }

    @Test
    @DisplayName("Creates an empty cart for the given user")
    void shouldCreateEmptyCartForUser() {
        Cart cart = Cart.create(USER_ID);

        assertThat(cart.getUserId()).isEqualTo(USER_ID);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Rejects creating a cart with a null user ID")
    void shouldRejectNullUserId() {
        Throwable exception = catchThrowable(() -> Cart.create(null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "[{index}] {0} + {1} = {2}")
    @DisplayName("Accumulates the quantity when the same product is added again")
    @CsvSource({"2, 3, 5", "2147483646, 1, 2147483647"})
    void shouldAccumulateQuantityForExistingProduct(int initialQuantity, int addedQuantity, int expectedQuantity) {
        Cart cart = cartWithProduct(initialQuantity);

        cart.addItem(PRODUCT_ID, new Quantity(addedQuantity));

        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(expectedQuantity)));
    }

    @Test
    @DisplayName("Keeps different products as separate cart items")
    void shouldKeepDifferentProductsAsSeparateItems() {
        Cart cart = cartWithProduct(2);

        cart.addItem(OTHER_PRODUCT_ID, new Quantity(3));

        assertThat(cart.getItems()).containsExactlyInAnyOrder(
                new CartItem(PRODUCT_ID, new Quantity(2)),
                new CartItem(OTHER_PRODUCT_ID, new Quantity(3))
        );
    }

    @Test
    @DisplayName("Rejects adding a new product with zero quantity")
    void shouldRejectAddingNewProductWithZeroQuantity() {
        Cart cart = Cart.create(USER_ID);

        Throwable exception = catchThrowable(() -> cart.addItem(PRODUCT_ID, Quantity.ZERO));

        assertThat(exception).isInstanceOf(CartItemQuantityMustBePositiveException.class);
        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Rejects adding zero quantity to an existing product without changing the cart")
    void shouldRejectAddingZeroQuantityToExistingProduct() {
        Cart cart = cartWithProduct(2);

        Throwable exception = catchThrowable(() -> cart.addItem(PRODUCT_ID, Quantity.ZERO));

        assertThat(exception).isInstanceOf(CartItemQuantityMustBePositiveException.class);
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Rejects adding a product with a null ID without changing the cart")
    void shouldRejectAddingNullProductId() {
        Cart cart = cartWithProduct(2);

        Throwable exception = catchThrowable(() -> cart.addItem(null, new Quantity(1)));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Rejects adding a null quantity without changing the cart")
    void shouldRejectAddingNullQuantity() {
        Cart cart = cartWithProduct(2);

        Throwable exception = catchThrowable(() -> cart.addItem(PRODUCT_ID, null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Rejects quantity overflow without changing the existing cart item")
    void shouldRejectQuantityOverflowWithoutChangingCart() {
        Cart cart = cartWithProduct(Integer.MAX_VALUE);

        Throwable exception = catchThrowable(() -> cart.addItem(PRODUCT_ID, new Quantity(1)));

        assertThat(exception).isExactlyInstanceOf(ArithmeticException.class);
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(Integer.MAX_VALUE)));
    }

    @ParameterizedTest(name = "[{index}] new quantity = {0}")
    @DisplayName("Replaces the quantity of an existing product and preserves other items")
    @ValueSource(ints = {1, 2, 5, Integer.MAX_VALUE})
    void shouldReplaceQuantityOfExistingProduct(int newQuantity) {
        Cart cart = cartWithProduct(2);
        cart.addItem(OTHER_PRODUCT_ID, new Quantity(3));

        cart.changeQuantity(PRODUCT_ID, new Quantity(newQuantity));

        assertThat(cart.getItems()).containsExactlyInAnyOrder(
                new CartItem(PRODUCT_ID, new Quantity(newQuantity)),
                new CartItem(OTHER_PRODUCT_ID, new Quantity(3))
        );
    }

    @Test
    @DisplayName("Rejects changing the quantity of a missing product without adding it")
    void shouldRejectChangingQuantityOfMissingProduct() {
        Cart cart = cartWithProduct(2);

        Throwable exception = catchThrowable(() -> cart.changeQuantity(OTHER_PRODUCT_ID, new Quantity(3)));

        assertThat(exception)
                .isInstanceOf(CartItemNotFoundException.class)
                .hasMessageContaining(OTHER_PRODUCT_ID.toString());
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Rejects changing an existing quantity to zero without removing the item")
    void shouldRejectChangingQuantityToZero() {
        Cart cart = cartWithProduct(2);

        Throwable exception = catchThrowable(() -> cart.changeQuantity(PRODUCT_ID, Quantity.ZERO));

        assertThat(exception).isInstanceOf(CartItemQuantityMustBePositiveException.class);
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Rejects changing quantity for a null product ID without changing the cart")
    void shouldRejectChangingQuantityForNullProductId() {
        Cart cart = cartWithProduct(2);

        Throwable exception = catchThrowable(() -> cart.changeQuantity(null, new Quantity(3)));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Rejects changing to a null quantity without changing the cart")
    void shouldRejectChangingToNullQuantity() {
        Cart cart = cartWithProduct(2);

        Throwable exception = catchThrowable(() -> cart.changeQuantity(PRODUCT_ID, null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Leaves the cart empty after removing its last item")
    void shouldRemoveLastItem() {
        Cart cart = cartWithProduct(2);

        cart.removeItem(PRODUCT_ID);

        assertThat(cart.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Removes only the requested product")
    void shouldRemoveOnlyRequestedProduct() {
        Cart cart = cartWithProduct(2);
        cart.addItem(OTHER_PRODUCT_ID, new Quantity(3));

        cart.removeItem(PRODUCT_ID);

        assertThat(cart.getItems()).containsExactly(new CartItem(OTHER_PRODUCT_ID, new Quantity(3)));
    }

    @Test
    @DisplayName("Keeps the cart unchanged when removing a missing product")
    void shouldIgnoreRemovingMissingProduct() {
        Cart cart = cartWithProduct(2);

        cart.removeItem(OTHER_PRODUCT_ID);

        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Rejects removing a null product ID without changing the cart")
    void shouldRejectRemovingNullProductId() {
        Cart cart = cartWithProduct(2);

        Throwable exception = catchThrowable(() -> cart.removeItem(null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Returns an unmodifiable list of cart items")
    void shouldReturnUnmodifiableItems() {
        Cart cart = cartWithProduct(2);
        List<CartItem> items = cart.getItems();

        Throwable exception = catchThrowable(items::clear);

        assertThat(exception).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThat(cart.getItems()).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
    }

    @Test
    @DisplayName("Keeps previously returned items unchanged after the cart is modified")
    void shouldReturnIndependentSnapshotOfItems() {
        Cart cart = cartWithProduct(2);
        List<CartItem> snapshot = cart.getItems();

        cart.changeQuantity(PRODUCT_ID, new Quantity(5));
        cart.addItem(OTHER_PRODUCT_ID, new Quantity(3));

        assertThat(snapshot).containsExactly(new CartItem(PRODUCT_ID, new Quantity(2)));
        assertThat(cart.getItems()).containsExactlyInAnyOrder(
                new CartItem(PRODUCT_ID, new Quantity(5)),
                new CartItem(OTHER_PRODUCT_ID, new Quantity(3))
        );
    }

    @Test
    @DisplayName("Identifies carts by user ID regardless of their contents")
    void shouldTreatCartsForSameUserAsEqual() {
        Cart populatedCart = cartWithProduct(2);
        Cart emptyCart = Cart.create(USER_ID);

        assertThat(populatedCart).isEqualTo(emptyCart).hasSameHashCodeAs(emptyCart);
    }

    @Test
    @DisplayName("Distinguishes carts belonging to different users")
    void shouldDistinguishCartsForDifferentUsers() {
        Cart firstCart = Cart.create(USER_ID);
        Cart secondCart = Cart.create(UUID.fromString("00000000-0000-0000-0000-000000000004"));

        assertThat(firstCart).isNotEqualTo(secondCart);
    }

    private static Cart cartWithProduct(int quantity) {
        Cart cart = Cart.create(USER_ID);
        cart.addItem(PRODUCT_ID, new Quantity(quantity));
        return cart;
    }
}
