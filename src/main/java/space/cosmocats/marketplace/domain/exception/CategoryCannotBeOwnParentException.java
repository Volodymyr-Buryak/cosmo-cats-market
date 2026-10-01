package space.cosmocats.marketplace.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.BusinessRuleException;

public final class CategoryCannotBeOwnParentException extends BusinessRuleException {
    private static final String MESSAGE_KEY = "error.category.cannot-be-own-parent";

    public CategoryCannotBeOwnParentException(UUID categoryId) {
        super(MESSAGE_KEY, categoryId);
    }
}
