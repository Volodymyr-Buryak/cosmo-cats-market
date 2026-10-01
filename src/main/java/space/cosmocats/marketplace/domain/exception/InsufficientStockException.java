package space.cosmocats.marketplace.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.BusinessRuleException;

public final class InsufficientStockException extends BusinessRuleException {
    private static final String MESSAGE_KEY = "error.product.insufficient-stock";

    public InsufficientStockException(UUID productId, int available, int requested) {
        super(MESSAGE_KEY, productId, available, requested);
    }
}
