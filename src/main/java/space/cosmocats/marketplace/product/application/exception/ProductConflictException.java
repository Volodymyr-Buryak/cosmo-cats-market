package space.cosmocats.marketplace.product.application.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.ConflictException;

public final class ProductConflictException extends ConflictException {
    private static final String MESSAGE_KEY = "error.product.conflict";
    private static final String MESSAGE = "Product operation conflicts with existing data: productId=%s";

    public ProductConflictException(UUID id) {
        super(MESSAGE_KEY, String.format(MESSAGE, id));
    }

    public ProductConflictException(UUID id, Throwable cause) {
        this(id);
        initCause(cause);
    }
}
