package space.cosmocats.marketplace.cart.domain.model.value;

import lombok.NonNull;
import java.util.UUID;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;

public record CartItem(
        @NonNull UUID productId,
        @NonNull Quantity quantity
) {
    public CartItem {
        if (quantity.isZero()) {
            throw new IllegalArgumentException("Cart item quantity must be greater than zero");
        }
    }

    public CartItem changeQuantity(Quantity newQuantity) {
        return new CartItem(productId, newQuantity);
    }
}
