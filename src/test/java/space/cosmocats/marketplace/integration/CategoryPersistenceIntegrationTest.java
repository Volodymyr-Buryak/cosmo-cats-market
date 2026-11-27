package space.cosmocats.marketplace.integration;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import space.cosmocats.marketplace.product.domain.model.Category;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import space.cosmocats.marketplace.product.application.service.CategoryService;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import space.cosmocats.marketplace.product.application.service.impl.CategoryServiceImpl;
import space.cosmocats.marketplace.product.application.exception.CategoryConflictException;
import space.cosmocats.marketplace.product.application.exception.CategoryNotFoundException;
import space.cosmocats.marketplace.product.domain.exception.CategoryHierarchyCycleException;
import space.cosmocats.marketplace.product.persistence.mapper.CategoryPersistenceMapperImpl;
import space.cosmocats.marketplace.product.persistence.repository.CategoryRepositoryAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({CategoryRepositoryAdapter.class, CategoryPersistenceMapperImpl.class, CategoryServiceImpl.class})
class CategoryPersistenceIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID USED_CATEGORY_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final UUID MISSING_CATEGORY_ID = UUID.fromString("40000000-0000-4000-8000-000000000099");

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepositoryAdapter categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Persists a category hierarchy with domain-assigned IDs")
    void shouldPersistCategoryHierarchy() {
        Category root = categoryService.createCategory(validCategoryBuilder().name("Root").build());
        Category child = validCategoryBuilder().name("Child").parentId(root.getId()).build();

        Category created = categoryService.createCategory(child);
        entityManager.clear();
        Category persisted = categoryService.getCategoryById(child.getId());

        assertThat(created).usingRecursiveComparison().isEqualTo(child);
        assertThat(persisted).usingRecursiveComparison().isEqualTo(child);
        assertThat(categoryService.getCategoryById(root.getId()).getParentId()).isNull();
    }

    @Test
    @DisplayName("Lists categories in name order")
    void shouldListCategoriesInNameOrder() {
        categoryService.createCategory(validCategoryBuilder().name("Root").build());
        categoryService.createCategory(validCategoryBuilder().name("Child").build());
        entityManager.clear();

        List<Category> categories = categoryService.getAllCategories();

        assertThat(categories).extracting(Category::getName).containsSubsequence("Child", "Root");
    }

    @Test
    @DisplayName("Updates a category and clears its parent without changing the ID")
    void shouldUpdateCategoryAndClearParent() {
        Category root = categoryService.createCategory(validCategoryBuilder().name("Root").build());
        Category child = categoryService.createCategory(
                validCategoryBuilder().name("Child").parentId(root.getId()).build()
        );
        Category changed = child.toBuilder()
                .description("Changed description")
                .parentId(null)
                .build();

        Category updated = categoryService.updateCategory(changed);
        entityManager.clear();
        Category persisted = categoryService.getCategoryById(child.getId());

        assertThat(updated).usingRecursiveComparison().isEqualTo(changed);
        assertThat(persisted).usingRecursiveComparison().isEqualTo(changed);
    }

    @Test
    @DisplayName("Deletes an unused category")
    void shouldDeleteUnusedCategory() {
        Category category = categoryService.createCategory(validCategoryBuilder().build());

        categoryService.deleteCategory(category.getId());
        entityManager.clear();

        assertThat(categoryRepository.findById(category.getId())).isEmpty();
    }

    @Test
    @DisplayName("Throws CategoryNotFoundException when reading an unknown category")
    void shouldThrowWhenCategoryIsNotFoundById() {
        Category missing = validCategoryBuilder().build();

        Throwable exception = catchThrowable(() -> categoryService.getCategoryById(missing.getId()));

        assertThat(exception).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("Throws CategoryNotFoundException when updating an unknown category")
    void shouldThrowWhenCategoryIsNotFoundForUpdate() {
        Category missing = validCategoryBuilder().build();

        Throwable exception = catchThrowable(() -> categoryService.updateCategory(missing));

        assertThat(exception).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("Throws CategoryNotFoundException when deleting an unknown category")
    void shouldThrowWhenCategoryIsNotFoundForDeletion() {
        Category missing = validCategoryBuilder().build();

        Throwable exception = catchThrowable(() -> categoryService.deleteCategory(missing.getId()));

        assertThat(exception).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("Does not insert an unknown category through repository update")
    void shouldNotInsertUnknownCategoryOnUpdate() {
        Category missing = validCategoryBuilder().build();

        var result = categoryRepository.update(missing);

        assertThat(result).isEmpty();
        assertThat(categoryRepository.findById(missing.getId())).isEmpty();
    }

    @Test
    @DisplayName("Rejects a duplicate category name after domain normalization")
    void shouldRejectDuplicateNormalizedName() {
        categoryService.createCategory(validCategoryBuilder().name("Unique").build());
        Category duplicate = validCategoryBuilder().name("  Unique  ").build();

        Throwable exception = catchThrowable(() -> categoryService.createCategory(duplicate));

        assertThat(exception).isInstanceOf(CategoryConflictException.class);
    }

    @Test
    @DisplayName("Rejects a duplicate category ID without overwriting existing data")
    void shouldRejectDuplicateIdWithoutOverwritingCategory() {
        Category existing = categoryService.createCategory(validCategoryBuilder().name("Existing").build());
        Category replacement = existing.toBuilder().name("Replacement").build();

        Throwable exception = catchThrowable(() -> categoryService.createCategory(replacement));

        assertThat(exception).isInstanceOf(CategoryConflictException.class);
        assertThat(categoryService.getCategoryById(existing.getId()).getName()).isEqualTo("Existing");
    }

    @Test
    @DisplayName("Rejects renaming a category to an existing name")
    void shouldRejectRenamingToExistingName() {
        categoryService.createCategory(validCategoryBuilder().name("First").build());
        Category second = categoryService.createCategory(validCategoryBuilder().name("Second").build());
        Category renamed = second.toBuilder().name("First").build();

        Throwable exception = catchThrowable(() -> categoryService.updateCategory(renamed));

        assertThat(exception).isInstanceOf(CategoryConflictException.class);
    }

    @Test
    @DisplayName("Rejects creating a category with a missing parent")
    void shouldRejectMissingParentOnCreate() {
        Category category = validCategoryBuilder().parentId(MISSING_CATEGORY_ID).build();

        Throwable exception = catchThrowable(() -> categoryService.createCategory(category));

        assertThat(exception).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("Rejects assigning a missing parent to an existing category")
    void shouldRejectMissingParentOnUpdate() {
        Category root = categoryService.createCategory(validCategoryBuilder().build());
        Category changed = root.toBuilder().parentId(MISSING_CATEGORY_ID).build();

        Throwable exception = catchThrowable(() -> categoryService.updateCategory(changed));

        assertThat(exception).isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("Rejects moving a category below its own descendant")
    void shouldRejectHierarchyCycle() {
        Category root = categoryService.createCategory(validCategoryBuilder().name("Root").build());
        Category child = categoryService.createCategory(
                validCategoryBuilder().name("Child").parentId(root.getId()).build()
        );
        Category grandchild = categoryService.createCategory(
                validCategoryBuilder().name("Grandchild").parentId(child.getId()).build()
        );
        Category changed = root.toBuilder().parentId(grandchild.getId()).build();

        Throwable exception = catchThrowable(() -> categoryService.updateCategory(changed));

        assertThat(exception).isInstanceOf(CategoryHierarchyCycleException.class);
    }

    @Test
    @DisplayName("Rejects deleting a category that has children")
    void shouldRejectDeletingParentCategory() {
        Category parent = categoryService.createCategory(validCategoryBuilder().name("Parent").build());
        categoryService.createCategory(validCategoryBuilder().name("Child").parentId(parent.getId()).build());

        Throwable exception = catchThrowable(() -> categoryService.deleteCategory(parent.getId()));

        assertThat(exception).isInstanceOf(CategoryConflictException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Rejects deleting a category referenced by products")
    void shouldRejectDeletingCategoryUsedByProducts() {
        Throwable exception = catchThrowable(() -> categoryService.deleteCategory(USED_CATEGORY_ID));

        assertThat(exception).isInstanceOf(CategoryConflictException.class);
    }

    private static Category.CategoryBuilder validCategoryBuilder() {
        return Category.builder()
                .name("Cosmic equipment")
                .description("Equipment for space travel");
    }
}
