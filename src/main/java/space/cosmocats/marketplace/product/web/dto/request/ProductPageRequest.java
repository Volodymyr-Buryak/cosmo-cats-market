package space.cosmocats.marketplace.product.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import space.cosmocats.marketplace.product.domain.repository.ProductSort;

public record ProductPageRequest(
        @Min(value = 0, message = "{validation.product.page.min}")
        Integer page,

        @Min(value = 1, message = "{validation.product.size.min}")
        @Max(value = 100, message = "{validation.product.size.max}")
        Integer size,

        ProductSort sort
) {}
