package space.cosmocats.marketplace.product.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.BusinessRuleException;

public final class InsufficientStockException extends BusinessRuleException {
    private static final String MESSAGE_KEY = "error.product.insufficient-stock";
    private static final String MESSAGE_TEMPLATE =
            "Product stock is insufficient: productId=%s, available=%d, requested=%d";

    public InsufficientStockException(UUID productId, String productName, int available, int requested) {
        super(
                MESSAGE_KEY,
                String.format(MESSAGE_TEMPLATE, productId, available, requested),
                productName, available, requested
        );
    }
}
