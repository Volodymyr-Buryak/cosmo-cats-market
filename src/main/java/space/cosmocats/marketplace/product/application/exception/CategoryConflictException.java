package space.cosmocats.marketplace.product.application.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.ConflictException;

public final class CategoryConflictException extends ConflictException {
    private static final String MESSAGE_KEY = "error.category.conflict";
    private static final String MESSAGE = "Category operation conflicts with existing data: categoryId=%s";

    public CategoryConflictException(UUID id) {
        super(MESSAGE_KEY, String.format(MESSAGE, id));
    }

    public CategoryConflictException(UUID id, Throwable cause) {
        this(id);
        initCause(cause);
    }
}
