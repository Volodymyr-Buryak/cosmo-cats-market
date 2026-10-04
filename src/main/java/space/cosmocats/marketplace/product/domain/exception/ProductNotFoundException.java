package space.cosmocats.marketplace.product.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.NotFoundException;

public final class ProductNotFoundException extends NotFoundException {
    private static final String MESSAGE_KEY = "error.product.not-found";

    public ProductNotFoundException(UUID productId) {
        super(MESSAGE_KEY, productId);
    }
}
