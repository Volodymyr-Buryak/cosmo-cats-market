package space.cosmocats.marketplace.order.web.dto;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record OrderRequest(
        @NotNull
        UUID buyerId,

        @NotEmpty
        @Size(max = 100) List<@NotNull @Valid OrderItemRequest> items
) {}
