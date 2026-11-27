package space.cosmocats.marketplace.order.application.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.NotFoundException;

public final class OrderNotFoundException extends NotFoundException {
    private static final String MESSAGE_KEY = "error.order.not-found";
    private static final String MESSAGE = "Order was not found: orderId=%s";

    public OrderNotFoundException(UUID id) {
        super(MESSAGE_KEY, String.format(MESSAGE, id));
    }
}
