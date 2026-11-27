package space.cosmocats.marketplace.order.domain.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import space.cosmocats.marketplace.order.domain.model.Order;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;

public interface OrderRepository {
    List<Order> findAll();
    Optional<Order> findById(UUID id);
    Optional<Order> findByIdForUpdate(UUID id);
    Order create(Order order);
    boolean updateStatus(UUID id, OrderStatus status);
    boolean deleteById(UUID id);
}
