package space.cosmocats.marketplace.order.domain.model;

import java.util.UUID;

public record PopularProduct(UUID productId, String productName, long quantitySold) {}
