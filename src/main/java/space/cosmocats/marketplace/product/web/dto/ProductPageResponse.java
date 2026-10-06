package space.cosmocats.marketplace.product.web.dto;

import java.util.List;

public record ProductPageResponse(
        List<ProductResponse> content,
        boolean hasContent,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public ProductPageResponse {
        content = List.copyOf(content);
    }
}
