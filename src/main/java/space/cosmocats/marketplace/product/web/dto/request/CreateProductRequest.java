package space.cosmocats.marketplace.product.web.dto.request;

import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record CreateProductRequest(
        @NotBlank(message = "{validation.product.name.required}")
        @Size(min = 3, message = "{validation.product.name.min-size}")
        String name,
        String description,

        @NotNull(message = "{validation.product.price.required}")
        @DecimalMin(value = "0", inclusive = false, message = "{validation.product.price.positive}")
        BigDecimal price,

        @NotBlank(message = "{validation.product.currency.required}")
        String currency,

        @NotNull(message = "{validation.product.stock.required}")
        @PositiveOrZero(message = "{validation.product.stock.positive-or-zero}")
        Integer stock,

        @NotNull(message = "{validation.product.category-id.required}")
        UUID categoryId
) {}
