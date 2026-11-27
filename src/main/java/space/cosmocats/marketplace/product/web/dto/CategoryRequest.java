package space.cosmocats.marketplace.product.web.dto;

import java.util.UUID;
import jakarta.validation.constraints.*;

public record CategoryRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 500)
        String description,

        UUID parentId
) {}
