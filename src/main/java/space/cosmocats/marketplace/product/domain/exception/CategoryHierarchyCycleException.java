package space.cosmocats.marketplace.product.domain.exception;

import java.util.UUID;
import space.cosmocats.marketplace.common.exception.DomainRuleViolationException;

public final class CategoryHierarchyCycleException extends DomainRuleViolationException {
    private static final String MESSAGE_KEY = "error.category.hierarchy-cycle";
    private static final String MESSAGE = "Category hierarchy contains a cycle: categoryId=%s";

    public CategoryHierarchyCycleException(UUID id) {
        super(MESSAGE_KEY, String.format(MESSAGE, id));
    }
}
