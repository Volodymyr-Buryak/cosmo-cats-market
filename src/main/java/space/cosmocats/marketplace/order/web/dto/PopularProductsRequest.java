package space.cosmocats.marketplace.order.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

public record PopularProductsRequest(@Min(1) @Max(100) Integer limit) {}
