package space.cosmocats.marketplace.product.web.dto.response;

import java.util.List;

public record ProductPageResponse(
        List<ProductResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public ProductPageResponse {
        content = List.copyOf(content);
    }
}
