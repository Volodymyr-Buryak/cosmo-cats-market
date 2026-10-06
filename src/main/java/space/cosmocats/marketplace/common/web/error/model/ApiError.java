package space.cosmocats.marketplace.common.web.error.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"detail", "pointer"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String detail,
        String pointer
) {}
