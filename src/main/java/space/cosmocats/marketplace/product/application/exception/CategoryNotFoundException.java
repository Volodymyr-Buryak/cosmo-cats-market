package space.cosmocats.marketplace.product.application.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.NotFoundException;

public final class CategoryNotFoundException extends NotFoundException {
    private static final String MESSAGE_KEY = "error.category.not-found";
    private static final String MESSAGE = "Category was not found: categoryId=%s";

    public CategoryNotFoundException(UUID id) {
        super(MESSAGE_KEY, String.format(MESSAGE, id));
    }
}
