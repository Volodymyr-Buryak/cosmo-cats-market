package space.cosmocats.marketplace.order.application.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.ConflictException;

public final class OrderConflictException extends ConflictException {
    private static final String MESSAGE_KEY = "error.order.conflict";
    private static final String MESSAGE = "Order operation conflicts with existing data or order state: orderId=%s";

    public OrderConflictException(UUID id) {
        super(MESSAGE_KEY, String.format(MESSAGE, id));
    }

    public OrderConflictException(UUID id, Throwable cause) {
        this(id);
        initCause(cause);
    }
}
