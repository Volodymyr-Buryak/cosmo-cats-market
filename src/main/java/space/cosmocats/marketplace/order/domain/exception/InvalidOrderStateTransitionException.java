package space.cosmocats.marketplace.order.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.common.exception.BusinessRuleException;

public final class InvalidOrderStateTransitionException extends BusinessRuleException {
    private static final String MESSAGE_KEY = "error.order.invalid-state-transition";
    private static final String LOG_MESSAGE =
            "Order status transition is not allowed: orderId=%s, currentStatus=%s, targetStatus=%s";

    public InvalidOrderStateTransitionException(UUID orderId, OrderStatus currentStatus, OrderStatus targetStatus) {
        super(
                MESSAGE_KEY,
                String.format(LOG_MESSAGE, orderId, currentStatus, targetStatus),
                currentStatus, targetStatus
        );
    }
}
