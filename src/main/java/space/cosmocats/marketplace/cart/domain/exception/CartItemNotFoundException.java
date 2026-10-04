package space.cosmocats.marketplace.cart.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.NotFoundException;

public final class CartItemNotFoundException extends NotFoundException {
    private static final String MESSAGE_KEY = "error.cart.item-not-found";

    public CartItemNotFoundException(UUID productId) {
        super(MESSAGE_KEY, productId);
    }
}
