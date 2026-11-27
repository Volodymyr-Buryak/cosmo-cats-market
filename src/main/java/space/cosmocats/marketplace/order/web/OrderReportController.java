package space.cosmocats.marketplace.order.web;

import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import space.cosmocats.marketplace.order.application.service.OrderReportService;
import space.cosmocats.marketplace.order.web.dto.PopularProductsRequest;
import space.cosmocats.marketplace.order.web.dto.PopularProductResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class OrderReportController {
    private static final int DEFAULT_LIMIT = 10;
    private final OrderReportService reportService;
    private final OrderReportMapper reportMapper;

    @GetMapping("/popular-products")
    public ResponseEntity<List<PopularProductResponse>> getPopularProducts(@Valid @ModelAttribute PopularProductsRequest request) {
        int limit = (request.limit() == null) ? DEFAULT_LIMIT : request.limit();
        var products = reportService.getPopularProducts(limit);
        var response = products.stream().map(reportMapper::toResponse).toList();
        return ResponseEntity.ok(response);
    }
}
