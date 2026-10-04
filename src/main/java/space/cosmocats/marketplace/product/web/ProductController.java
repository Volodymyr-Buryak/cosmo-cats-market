package space.cosmocats.marketplace.product.web;

import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import space.cosmocats.marketplace.product.web.dto.response.ProductResponse;
import space.cosmocats.marketplace.product.web.dto.request.ProductPageRequest;
import space.cosmocats.marketplace.product.application.service.ProductService;
import space.cosmocats.marketplace.product.web.dto.request.CreateProductRequest;
import space.cosmocats.marketplace.product.web.dto.request.UpdateProductRequest;
import space.cosmocats.marketplace.product.web.dto.response.ProductPageResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductMapper productMapper;
    private final ProductService productService;

    @GetMapping
    public ProductPageResponse getAllProducts(@Valid @ModelAttribute ProductPageRequest request) {
        return productMapper.toResponse(
                productService.getAllProducts(productMapper.toCriteria(request))
        );
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable UUID id) {
        return productMapper.toResponse(
                productService.getProductById(id)
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productMapper.toResponse(
                productService.createProduct(productMapper.toDomain(request))
        );
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return productMapper.toResponse(
                productService.updateProduct(productMapper.toDomain(id, request))
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
    }

}