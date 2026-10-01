package space.cosmocats.marketplace.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.BusinessRuleException;

public final class CategoryCycleException extends BusinessRuleException {
    private static final String MESSAGE_KEY = "error.category.cycle";

    public CategoryCycleException(UUID categoryId, UUID newParentId) {
        super(MESSAGE_KEY, categoryId, newParentId);
    }
}
