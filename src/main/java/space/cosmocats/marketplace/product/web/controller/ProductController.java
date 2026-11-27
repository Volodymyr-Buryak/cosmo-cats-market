package space.cosmocats.marketplace.product.web.controller;

import java.net.URI;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import space.cosmocats.marketplace.product.web.dto.ProductRequest;
import space.cosmocats.marketplace.product.web.dto.ProductResponse;
import space.cosmocats.marketplace.product.web.mapper.ProductMapper;
import space.cosmocats.marketplace.product.web.dto.ProductPageRequest;
import space.cosmocats.marketplace.product.web.dto.ProductPageResponse;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import space.cosmocats.marketplace.product.application.service.ProductService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductMapper productMapper;
    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ProductPageResponse> getAllProducts(@Valid @ModelAttribute ProductPageRequest request) {
        var criteria = productMapper.toCriteria(request);
        var products = productService.getAllProducts(criteria);
        var response = productMapper.toPageResponse(products);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable UUID id) {
        var product = productService.getProductById(id);
        var response = productMapper.toResponse(product);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        var product = productMapper.toDomain(request);
        var createdProduct = productService.createProduct(product);
        var created = productMapper.toResponse(createdProduct);

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
        var product = productMapper.toDomain(id, request);
        var updatedProduct = productService.updateProduct(product);
        var response = productMapper.toResponse(updatedProduct);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

}
