package space.cosmocats.marketplace.order.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import space.cosmocats.marketplace.order.domain.model.Order;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.order.domain.repository.OrderRepository;
import space.cosmocats.marketplace.order.persistence.mapper.OrderPersistenceMapper;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {
    private final JpaOrderRepository jpaOrderRepository;
    private final OrderPersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public List<Order> findAll() {
        return jpaOrderRepository.findAll(Sort.by("createdAt").and(Sort.by("id")))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaOrderRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Order> findByIdForUpdate(UUID id) {
        return jpaOrderRepository.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public Order create(Order order) {
        var entity = mapper.toEntity(order);
        for (int position = 0; position < order.getItems().size(); position++) {
            entity.getItems().add(mapper.toItemEntity(order.getItems().get(position), entity, position));
        }
        entityManager.persist(entity);
        jpaOrderRepository.flush();
        entityManager.refresh(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public boolean updateStatus(UUID id, OrderStatus status) {
        return jpaOrderRepository.findById(id).map(entity -> {
            entity.setStatus(status);
            jpaOrderRepository.flush();
            return true;
        }).orElse(false);
    }

    @Override
    public boolean deleteById(UUID id) {
        return jpaOrderRepository.findById(id).map(entity -> {
            jpaOrderRepository.delete(entity);
            jpaOrderRepository.flush();
            return true;
        }).orElse(false);
    }
}
