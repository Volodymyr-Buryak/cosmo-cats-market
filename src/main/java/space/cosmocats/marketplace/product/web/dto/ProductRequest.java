package space.cosmocats.marketplace.product.web.dto;

import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import space.cosmocats.marketplace.product.web.validation.currency.CurrencyCode;
import space.cosmocats.marketplace.product.web.validation.cosmicword.CosmicWordCheck;

public record ProductRequest(
        @NotBlank
        @Size(min = 3, max = 100)
        @CosmicWordCheck
        String name,

        @NotBlank
        @Size(min = 20, max = 500)
        String description,

        @NotNull
        @Positive
        @Digits(integer = 17, fraction = 2)
        BigDecimal price,

        @NotBlank
        @CurrencyCode
        String currency,

        @NotNull
        @PositiveOrZero
        Integer stock,

        @NotNull
        UUID categoryId
) {}
