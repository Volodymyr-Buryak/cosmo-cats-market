package space.cosmocats.marketplace.cart.domain.exception;

import space.cosmocats.marketplace.common.exception.DomainRuleViolationException;

public final class CartItemQuantityMustBePositiveException extends DomainRuleViolationException {
    private static final String MESSAGE_KEY = "error.cart-item.quantity-must-be-positive";
    private static final String MESSAGE = "Cart item quantity must be greater than zero";

    public CartItemQuantityMustBePositiveException() {
        super(MESSAGE_KEY, MESSAGE);
    }
}
