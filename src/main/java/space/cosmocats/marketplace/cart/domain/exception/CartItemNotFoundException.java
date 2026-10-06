package space.cosmocats.marketplace.cart.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.NotFoundException;

public final class CartItemNotFoundException extends NotFoundException {
    private static final String MESSAGE_KEY = "error.cart.item-not-found";
    private static final String MESSAGE = "Cart item was not found: productId=%s";

    public CartItemNotFoundException(UUID productId) {
        super(MESSAGE_KEY, String.format(MESSAGE, productId));
    }
}
