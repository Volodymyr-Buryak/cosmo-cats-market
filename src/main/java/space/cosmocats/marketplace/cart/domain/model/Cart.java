package space.cosmocats.marketplace.cart.domain.model;

import lombok.*;
import java.util.*;
import space.cosmocats.marketplace.cart.domain.model.value.CartItem;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.cart.domain.exception.CartItemNotFoundException;
import space.cosmocats.marketplace.cart.domain.exception.CartItemQuantityMustBePositiveException;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Cart {

    @Getter
    @EqualsAndHashCode.Include
    private final UUID userId;
    private final Map<UUID, CartItem> items;

    private Cart(@NonNull UUID userId) {
        this.userId = userId;
        this.items = new LinkedHashMap<>();
    }

    public static Cart create(UUID userId) {
        return new Cart(userId);
    }

    public void addItem(@NonNull UUID productId, @NonNull Quantity quantity) {
        checkQuantity(quantity);
        items.compute(productId, (id, currentItem) ->
                Optional.ofNullable(currentItem)
                        .map(item -> item.changeQuantity(item.quantity().add(quantity)))
                        .orElseGet(() -> new CartItem(id, quantity))
        );
    }

    public void changeQuantity(@NonNull UUID productId, @NonNull Quantity newQuantity) {
        checkQuantity(newQuantity);
        items.compute(productId, (id, currentItem) ->
                Optional.ofNullable(currentItem)
                        .map(item -> item.changeQuantity(newQuantity))
                        .orElseThrow(() -> new CartItemNotFoundException(id))
        );
    }

    public void removeItem(@NonNull UUID productId) {
        items.remove(productId);
    }

    public List<CartItem> getItems() {
        return List.copyOf(items.values());
    }

    private static void checkQuantity(Quantity quantity) {
        if (quantity.isZero()) {
            throw new CartItemQuantityMustBePositiveException();
        }
    }
}
