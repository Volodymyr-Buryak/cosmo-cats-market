package space.cosmocats.marketplace.order.application.service;

import java.util.List;
import java.util.UUID;
import space.cosmocats.marketplace.order.domain.model.Order;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.order.application.command.CreateOrderCommand;

public interface OrderService {
    List<Order> getAllOrders();
    Order getOrderById(UUID id);
    Order createOrder(Order order);
    Order createOrder(CreateOrderCommand command);
    Order updateStatus(UUID id, OrderStatus status);
    Order updateOrder(Order order);
    void deleteOrder(UUID id);
}
