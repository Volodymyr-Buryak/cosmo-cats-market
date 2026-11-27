package space.cosmocats.marketplace.product.domain.exception;

import space.cosmocats.marketplace.common.exception.DomainRuleViolationException;

public final class ProductPriceMustBePositiveException extends DomainRuleViolationException {
    private static final String MESSAGE_KEY = "error.product.price-must-be-positive";
    private static final String MESSAGE = "Product price must be greater than zero";

    public ProductPriceMustBePositiveException() {
        super(MESSAGE_KEY, MESSAGE);
    }
}
