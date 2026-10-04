package space.cosmocats.marketplace.product.domain.repository;

public record ProductCriteria(
        int page,
        int size,
        ProductSort sort
) {}
