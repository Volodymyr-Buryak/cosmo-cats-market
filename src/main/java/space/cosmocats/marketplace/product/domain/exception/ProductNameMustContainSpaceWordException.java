package space.cosmocats.marketplace.product.domain.exception;

import space.cosmocats.marketplace.common.exception.BusinessRuleException;

public final class ProductNameMustContainSpaceWordException extends BusinessRuleException {
    private static final String MESSAGE_KEY = "error.product.name-must-contain-space-word";

    public ProductNameMustContainSpaceWordException() {
        super(MESSAGE_KEY);
    }
}
