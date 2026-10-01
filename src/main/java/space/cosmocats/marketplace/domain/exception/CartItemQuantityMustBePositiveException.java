package space.cosmocats.marketplace.domain.exception;

import space.cosmocats.marketplace.common.exception.BusinessRuleException;

public final class CartItemQuantityMustBePositiveException extends BusinessRuleException {
    private static final String MESSAGE_KEY = "error.cart-item.quantity-must-be-positive";

    public CartItemQuantityMustBePositiveException() {
        super(MESSAGE_KEY);
    }
}