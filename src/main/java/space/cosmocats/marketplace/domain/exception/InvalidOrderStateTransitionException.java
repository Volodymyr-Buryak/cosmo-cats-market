package space.cosmocats.marketplace.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.BusinessRuleException;
import space.cosmocats.marketplace.domain.model.OrderStatus;

public final class InvalidOrderStateTransitionException extends BusinessRuleException {
    private static final String MESSAGE_KEY = "error.order.invalid-state-transition";

    public InvalidOrderStateTransitionException(UUID orderId, OrderStatus currentStatus, OrderStatus targetStatus) {
        super(MESSAGE_KEY, orderId, currentStatus, targetStatus);
    }
}
