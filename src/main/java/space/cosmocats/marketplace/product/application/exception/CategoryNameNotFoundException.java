package space.cosmocats.marketplace.product.application.exception;

import space.cosmocats.marketplace.common.exception.NotFoundException;

public final class CategoryNameNotFoundException extends NotFoundException {
    private static final String MESSAGE_KEY = "error.category.name-not-found";
    private static final String MESSAGE = "Category not found: name=%s";

    public CategoryNameNotFoundException(String name) {
        super(MESSAGE_KEY, String.format(MESSAGE, name), name);
    }
}
