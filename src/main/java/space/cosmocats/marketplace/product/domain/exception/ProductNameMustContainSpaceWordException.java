package space.cosmocats.marketplace.product.domain.exception;

import space.cosmocats.marketplace.common.exception.DomainRuleViolationException;

public final class ProductNameMustContainSpaceWordException extends DomainRuleViolationException {
    private static final String MESSAGE_KEY = "error.product.name-must-contain-space-word";

    public ProductNameMustContainSpaceWordException() {
        super(MESSAGE_KEY, "Product name does not contain a required cosmic word");
    }
}
