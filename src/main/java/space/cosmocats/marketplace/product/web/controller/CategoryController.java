package space.cosmocats.marketplace.product.web.controller;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import space.cosmocats.marketplace.product.web.dto.CategoryRequest;
import space.cosmocats.marketplace.product.web.dto.CategoryResponse;
import space.cosmocats.marketplace.product.web.mapper.CategoryMapper;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import space.cosmocats.marketplace.product.application.service.CategoryService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryMapper categoryMapper;
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        var response = categoryService.getAllCategories().stream()
                .map(categoryMapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-name")
    public ResponseEntity<CategoryResponse> getCategoryByName(
            @RequestParam @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 100) String name
    ) {
        var category = categoryService.findCategoryByName(name)
                .orElseThrow(() -> new space.cosmocats.marketplace.product.application.exception.CategoryNameNotFoundException(name));
        var response = categoryMapper.toResponse(category);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable UUID id) {
        var category = categoryService.getCategoryById(id);
        var response = categoryMapper.toResponse(category);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        var category = categoryMapper.toDomain(request);
        var createdCategory = categoryService.createCategory(category);
        var created = categoryMapper.toResponse(createdCategory);

        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location)
                .body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryRequest request
    ) {
        var category = categoryMapper.toDomain(id, request);
        var updatedCategory = categoryService.updateCategory(category);
        var response = categoryMapper.toResponse(updatedCategory);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

}
