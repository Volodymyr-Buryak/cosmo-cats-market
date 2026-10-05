package space.cosmocats.marketplace.order.domain.exception;

import space.cosmocats.marketplace.common.exception.DomainRuleViolationException;

public final class EmptyOrderException extends DomainRuleViolationException {
    private static final String MESSAGE_KEY = "error.order.empty";

    public EmptyOrderException() {
        super(MESSAGE_KEY, "Order creation failed because the order contains no items");
    }
}
