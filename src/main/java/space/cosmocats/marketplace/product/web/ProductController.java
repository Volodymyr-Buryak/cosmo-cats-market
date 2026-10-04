package space.cosmocats.marketplace.product.web;

import java.net.URI;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import space.cosmocats.marketplace.product.web.dto.request.ProductRequest;
import space.cosmocats.marketplace.product.web.dto.response.ProductResponse;
import space.cosmocats.marketplace.product.web.dto.request.ProductPageRequest;
import space.cosmocats.marketplace.product.application.service.ProductService;
import space.cosmocats.marketplace.product.web.dto.response.ProductPageResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductMapper productMapper;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductPageResponse> getAllProducts(@Valid @ModelAttribute ProductPageRequest request) {
        return ResponseEntity.ok(
                productMapper.toPageResponse(productService.getAllProducts(productMapper.toCriteria(request)))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(productMapper.toResponse(productService.getProductById(id)));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productMapper.toResponse(
                productService.createProduct(productMapper.toDomain(UUID.randomUUID(), request))
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(
                productMapper.toResponse(productService.updateProduct(productMapper.toDomain(id, request)))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}