package space.cosmocats.marketplace.product.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import space.cosmocats.marketplace.product.domain.repository.ProductSort;

public record ProductPageRequest(
        @PositiveOrZero
        Integer page,

        @Positive
        @Max(value = 100)
        Integer size,
        ProductSort sort
) {}
