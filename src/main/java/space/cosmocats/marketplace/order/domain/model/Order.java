package space.cosmocats.marketplace.order.domain.model;

import lombok.*;
import java.util.*;
import java.time.Instant;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.order.domain.model.value.OrderItem;
import space.cosmocats.marketplace.order.domain.exception.EmptyOrderException;
import space.cosmocats.marketplace.order.domain.exception.InvalidOrderStateTransitionException;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Order {

    @EqualsAndHashCode.Include
    private final UUID id;
    private final UUID buyerId;
    private final List<OrderItem> items;
    private final Money total;
    private final Instant createdAt;
    private OrderStatus status;

    @Builder
    private Order(
            UUID id,
            @NonNull UUID buyerId,
            @NonNull List<OrderItem> items,
            @NonNull Instant createdAt,
            OrderStatus status
    ) {
        this.buyerId = buyerId;
        this.createdAt = createdAt;

        this.items = checkAndCopyItems(items);
        this.total = calculateTotal(this.items);

        this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        this.status = Objects.requireNonNullElse(status, OrderStatus.CREATED);
    }

    private static List<OrderItem> checkAndCopyItems(List<OrderItem> items) {
        return Optional.of(items)
                .filter(orderItems -> !orderItems.isEmpty())
                .map(List::copyOf)
                .orElseThrow(EmptyOrderException::new);
    }

    private static Money calculateTotal(List<OrderItem> items) {
        Currency currency = items.getFirst().unitPrice().currency();
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(Money.zero(currency), Money::add);
    }

    public void markAsPaid() {
        changeStatus(OrderStatus.CREATED, OrderStatus.PAID);
    }

    public void markAsShipped() {
        changeStatus(OrderStatus.PAID, OrderStatus.SHIPPED);
    }

    public void markAsDelivered() {
        changeStatus(OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    }

    public void cancel() {
        if (!(status == OrderStatus.CREATED || status == OrderStatus.PAID)) {
            throw new InvalidOrderStateTransitionException(id, status, OrderStatus.CANCELLED);
        }
        status = OrderStatus.CANCELLED;
    }

    private void changeStatus(OrderStatus expectedStatus, OrderStatus newStatus) {
        if (status != expectedStatus) {
            throw new InvalidOrderStateTransitionException(id, status, newStatus);
        }
        status = newStatus;
    }
}