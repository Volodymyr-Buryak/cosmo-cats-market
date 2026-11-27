package space.cosmocats.marketplace.order.web.dto;

import java.util.UUID;
import java.math.BigDecimal;

public record OrderItemResponse(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        String currency,
        int quantity
) {}
