package space.cosmocats.marketplace.order.web.dto;

import java.util.UUID;

public record PopularProductResponse(UUID productId, String productName, long quantitySold) {}
