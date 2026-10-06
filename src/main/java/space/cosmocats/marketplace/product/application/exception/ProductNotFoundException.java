package space.cosmocats.marketplace.product.application.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.NotFoundException;

public final class ProductNotFoundException extends NotFoundException {
    private static final String MESSAGE_KEY = "error.product.not-found";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "Product was not found: productId=%s";

    public ProductNotFoundException(UUID productId) {
        super(MESSAGE_KEY, String.format(PRODUCT_NOT_FOUND_MESSAGE, productId));
    }
}
