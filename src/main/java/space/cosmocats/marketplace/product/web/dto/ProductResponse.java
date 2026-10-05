package space.cosmocats.marketplace.product.web.dto;

import java.util.UUID;
import java.math.BigDecimal;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        int stock,
        UUID categoryId
) {}
