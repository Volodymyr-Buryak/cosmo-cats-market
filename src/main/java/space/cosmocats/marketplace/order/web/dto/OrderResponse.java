package space.cosmocats.marketplace.order.web.dto;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;

public record OrderResponse(UUID id, UUID buyerId, Instant createdAt, OrderStatus status,
                            List<OrderItemResponse> items, BigDecimal total, String currency) {}
