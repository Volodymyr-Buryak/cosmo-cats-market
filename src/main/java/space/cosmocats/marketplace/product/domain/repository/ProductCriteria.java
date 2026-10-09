package space.cosmocats.marketplace.product.domain.repository;

import lombok.NonNull;

public record ProductCriteria(
        int page,
        int size,
        @NonNull ProductSort sort
) {
    public ProductCriteria {
        if (page < 0) throw new IllegalArgumentException("Page must not be negative");
        if (size <= 0) throw new IllegalArgumentException("Size must be greater than zero");
    }
}