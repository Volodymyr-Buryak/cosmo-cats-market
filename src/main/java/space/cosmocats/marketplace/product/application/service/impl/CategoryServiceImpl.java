package space.cosmocats.marketplace.product.application.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import space.cosmocats.marketplace.product.domain.model.Category;
import space.cosmocats.marketplace.product.application.service.CategoryService;
import space.cosmocats.marketplace.product.domain.repository.CategoryRepository;
import space.cosmocats.marketplace.product.application.exception.CategoryConflictException;
import space.cosmocats.marketplace.product.application.exception.CategoryNotFoundException;
import space.cosmocats.marketplace.product.domain.exception.CategoryHierarchyCycleException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category getCategoryById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Override
    public java.util.Optional<Category> findCategoryByName(String name) {
        return categoryRepository.findByName(name.strip());
    }

    @Override
    @Transactional
    public Category createCategory(Category category) {
        categoryRepository.lockForMutation();

        if (categoryRepository.findById(category.getId()).isPresent()) {
            throw new CategoryConflictException(category.getId());
        }

        validateCategory(category);

        try {
            return categoryRepository.create(category);
        } catch (DataIntegrityViolationException ex) {
            throw new CategoryConflictException(category.getId(), ex);
        }

    }

    @Override
    @Transactional
    public Category updateCategory(Category category) {
        categoryRepository.lockForMutation();

        if (categoryRepository.findById(category.getId()).isEmpty()) {
            throw new CategoryNotFoundException(category.getId());
        }

        validateCategory(category);

        try {
            return categoryRepository.update(category)
                    .orElseThrow(() -> new CategoryNotFoundException(category.getId()));
        } catch (DataIntegrityViolationException ex) {
            throw new CategoryConflictException(category.getId(), ex);
        }
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        categoryRepository.lockForMutation();

        try {
            if (!categoryRepository.deleteById(id)) {
                throw new CategoryNotFoundException(id);
            }
        } catch (DataIntegrityViolationException ex) {
            throw new CategoryConflictException(id, ex);
        }

    }

    private void validateCategory(Category category) {
        if (categoryRepository.existsByNameAndIdNot(category.getName(), category.getId())) {
            throw new CategoryConflictException(category.getId());
        }

        var visited = new HashSet<UUID>();
        visited.add(category.getId());

        UUID ancestorId = category.getParentId();

        while (ancestorId != null) {
            if (!visited.add(ancestorId)) {
                throw new CategoryHierarchyCycleException(category.getId());
            }
            UUID currentId = ancestorId;
            ancestorId = categoryRepository.findById(currentId)
                    .orElseThrow(() -> new CategoryNotFoundException(currentId)).getParentId();
        }

    }
}
