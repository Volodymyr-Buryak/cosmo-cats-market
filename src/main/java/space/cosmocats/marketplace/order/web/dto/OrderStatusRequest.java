package space.cosmocats.marketplace.order.web.dto;

import jakarta.validation.constraints.NotNull;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;

public record OrderStatusRequest(
        @NotNull
        OrderStatus status
) {}
