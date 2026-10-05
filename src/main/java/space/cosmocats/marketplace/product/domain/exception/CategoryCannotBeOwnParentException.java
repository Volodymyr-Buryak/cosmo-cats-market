package space.cosmocats.marketplace.product.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.BusinessRuleException;

public final class CategoryCannotBeOwnParentException extends BusinessRuleException {
    private static final String MESSAGE_KEY = "error.category.cannot-be-own-parent";
    private static final String MESSAGE = "Category cannot be assigned as its own parent: categoryId=%s";

    public CategoryCannotBeOwnParentException(UUID categoryId, String categoryName) {
        super(MESSAGE_KEY, String.format(MESSAGE, categoryId), categoryName);
    }
}
