package space.cosmocats.marketplace.order.application.service.impl;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import space.cosmocats.marketplace.order.domain.model.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.order.domain.model.value.OrderItem;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.order.application.service.OrderService;
import space.cosmocats.marketplace.order.domain.repository.OrderRepository;
import space.cosmocats.marketplace.product.application.service.ProductService;
import space.cosmocats.marketplace.order.application.command.CreateOrderCommand;
import space.cosmocats.marketplace.order.application.exception.OrderConflictException;
import space.cosmocats.marketplace.order.application.exception.OrderNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final ProductService productService;
    private final OrderRepository orderRepository;

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    @Transactional
    public Order createOrder(CreateOrderCommand command) {
        List<OrderItem> items = new ArrayList<>();

        for (var item : command.items()) {
            var product = productService.getProductById(item.productId());
            items.add(OrderItem.of(product, new Quantity(item.quantity())));
        }

        Order order = Order.builder()
                .buyerId(command.buyerId())
                .items(items)
                .createdAt(Instant.now())
                .build();

        return persistNewOrder(order);
    }

    @Override
    @Transactional
    public Order updateStatus(UUID id, OrderStatus status) {
        Order stored = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return applyStatus(stored, status);
    }

    @Override
    @Transactional
    public Order createOrder(Order order) {
        return persistNewOrder(order);
    }

    private Order persistNewOrder(Order order) {
        if (order.getStatus() != OrderStatus.CREATED) {
            throw new OrderConflictException(order.getId());
        }

        if (orderRepository.findById(order.getId()).isPresent()) {
            throw new OrderConflictException(order.getId());
        }

        try {
            return orderRepository.create(order);
        } catch (DataIntegrityViolationException ex) {
            throw new OrderConflictException(order.getId(), ex);
        }
    }

    @Override
    @Transactional
    public Order updateOrder(Order order) {
        Order stored = orderRepository.findByIdForUpdate(order.getId())
                .orElseThrow(() -> new OrderNotFoundException(order.getId()));

        if (hasChangedPurchaseDetails(stored, order)) {
            throw new OrderConflictException(order.getId());
        }

        return applyStatus(stored, order.getStatus());
    }

    private Order applyStatus(Order stored, OrderStatus status) {
        if (stored.getStatus() == status) {
            return stored;
        }

        switch (status) {
            case PAID -> stored.markAsPaid();
            case SHIPPED -> stored.markAsShipped();
            case DELIVERED -> stored.markAsDelivered();
            case CANCELLED -> stored.cancel();
            case CREATED -> throw new OrderConflictException(stored.getId());
        }

        if (!orderRepository.updateStatus(stored.getId(), stored.getStatus())) {
            throw new OrderNotFoundException(stored.getId());
        }

        return stored;
    }

    @Override
    @Transactional
    public void deleteOrder(UUID id) {
        Order stored = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (stored.getStatus() != OrderStatus.CREATED && stored.getStatus() != OrderStatus.CANCELLED) {
            throw new OrderConflictException(id);
        }

        if (!orderRepository.deleteById(id)) {
            throw new OrderNotFoundException(id);
        }
    }

    private boolean hasChangedPurchaseDetails(Order stored, Order updated) {
        return !stored.getBuyerId().equals(updated.getBuyerId())
                || !stored.getCreatedAt().equals(updated.getCreatedAt())
                || !stored.getItems().equals(updated.getItems());
    }
}
