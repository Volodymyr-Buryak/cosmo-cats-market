package space.cosmocats.marketplace.product.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import space.cosmocats.marketplace.product.domain.model.Category;
import space.cosmocats.marketplace.product.domain.repository.CategoryRepository;
import space.cosmocats.marketplace.product.persistence.mapper.CategoryPersistenceMapper;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final EntityManager entityManager;
    private final CategoryPersistenceMapper mapper;
    private final JpaCategoryRepository jpaCategoryRepository;

    @Override
    public List<Category> findAll() {
        return jpaCategoryRepository.findAll(Sort.by("name").and(Sort.by("id")))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaCategoryRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Category> findByName(String name) {
        return jpaCategoryRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, UUID id) {
        return jpaCategoryRepository.existsByNameAndIdNot(name, id);
    }

    @Override
    public Category create(Category category) {
        var entity = mapper.toEntity(category);
        entityManager.persist(entity);
        entityManager.flush();
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<Category> update(Category category) {
        return jpaCategoryRepository.findById(category.getId()).map(entity -> {
            mapper.updateEntity(category, entity);
            jpaCategoryRepository.flush();
            return mapper.toDomain(entity);
        });
    }

    @Override
    public boolean deleteById(UUID id) {
        return jpaCategoryRepository.findById(id).map(entity -> {
            jpaCategoryRepository.delete(entity);
            jpaCategoryRepository.flush();
            return true;
        }).orElse(false);
    }

    // Serialize hierarchy changes to prevent cycles caused by concurrent updates.
    @Override
    public void lockForMutation() {
        entityManager.createNativeQuery("LOCK TABLE public.categories IN SHARE ROW EXCLUSIVE MODE")
                .executeUpdate();
    }

}
