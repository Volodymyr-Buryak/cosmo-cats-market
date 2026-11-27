package space.cosmocats.marketplace.order.web.dto;

import java.util.UUID;
import jakarta.validation.constraints.*;

public record OrderItemRequest(
        @NotNull
        UUID productId,

        @NotNull
        @Positive
        Integer quantity
) {}
