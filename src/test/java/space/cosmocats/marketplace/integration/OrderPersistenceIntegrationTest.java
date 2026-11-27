package space.cosmocats.marketplace.integration;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import java.util.Currency;
import java.math.BigDecimal;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import space.cosmocats.marketplace.order.domain.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.order.domain.model.value.OrderItem;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import space.cosmocats.marketplace.order.application.service.OrderService;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import space.cosmocats.marketplace.order.application.service.impl.OrderServiceImpl;
import space.cosmocats.marketplace.order.application.exception.OrderConflictException;
import space.cosmocats.marketplace.order.application.exception.OrderNotFoundException;
import space.cosmocats.marketplace.order.persistence.mapper.OrderPersistenceMapperImpl;
import space.cosmocats.marketplace.order.persistence.repository.OrderRepositoryAdapter;
import space.cosmocats.marketplace.order.domain.exception.InvalidOrderStateTransitionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({OrderRepositoryAdapter.class, OrderPersistenceMapperImpl.class, OrderServiceImpl.class})
class OrderPersistenceIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_PRODUCT_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID MISSING_PRODUCT_ID = UUID.fromString("30000000-0000-4000-8000-000000000099");
    private static final UUID BUYER_ID = UUID.fromString("50000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_BUYER_ID = UUID.fromString("50000000-0000-4000-8000-000000000002");
    private static final Instant CREATED_AT = Instant.parse("2026-11-25T12:00:00.123456Z");
    private static final Currency EUR = Currency.getInstance("EUR");

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepositoryAdapter orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private space.cosmocats.marketplace.product.application.service.ProductService productService;

    @Test
    @DisplayName("Persists repeated products, purchase prices and the original item order")
    void shouldPersistPurchaseDetailsAndItemOrder() {
        List<OrderItem> items = List.of(
                orderItem(OTHER_PRODUCT_ID, "First snapshot", "1.234", 2),
                orderItem(PRODUCT_ID, "Second snapshot", "3.14159", 3),
                orderItem(OTHER_PRODUCT_ID, "Another price", "0.001", 1)
        );
        Order order = validOrderBuilder().items(items).build();

        Order created = orderService.createOrder(order);
        entityManager.clear();
        Order persisted = orderService.getOrderById(order.getId());
        List<Long> itemIds = findItemIds(order.getId());
        String currency = jdbcTemplate.queryForObject(
                "SELECT currency FROM orders WHERE id = ?", String.class, order.getId()
        );

        assertThat(created).usingRecursiveComparison().isEqualTo(order);
        assertThat(persisted).usingRecursiveComparison().isEqualTo(order);
        assertThat(persisted.getTotal().amount()).isEqualByComparingTo("11.89377");
        assertThat(itemIds).hasSize(3).doesNotHaveDuplicates().allMatch(id -> id > 0);
        assertThat(currency).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Preserves purchase snapshots when the catalog product changes")
    void shouldPreserveSnapshotsAfterProductChanges() {
        Order order = validOrderBuilder().items(List.of(
                orderItem(OTHER_PRODUCT_ID, "Original product name", "1.234", 2)
        )).build();
        Order created = orderService.createOrder(order);

        jdbcTemplate.update("""
                UPDATE products
                SET name = 'Star Updated', price_amount = 1000
                WHERE id = ?
                """, OTHER_PRODUCT_ID);
        entityManager.clear();
        Order persisted = orderService.getOrderById(created.getId());

        assertThat(persisted.getItems()).containsExactlyElementsOf(order.getItems());
    }

    @Test
    @DisplayName("Returns the stored timestamp precision so the created order can be updated")
    void shouldReturnDatabaseTimestampPrecisionOnCreate() {
        Instant nanosecondTimestamp = Instant.parse("2026-11-25T12:00:00.123456789Z");
        Order order = validOrderBuilder().createdAt(nanosecondTimestamp).build();

        Order created = orderService.createOrder(order);
        entityManager.clear();
        Order persisted = orderService.getOrderById(created.getId());
        created.markAsPaid();
        Order updated = orderService.updateOrder(created);

        assertThat(persisted.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("Lists orders in chronological order")
    void shouldListOrdersInChronologicalOrder() {
        Order first = orderService.createOrder(validOrderBuilder().build());
        Order second = orderService.createOrder(
                validOrderBuilder().createdAt(CREATED_AT.plusSeconds(1)).build()
        );
        entityManager.clear();

        List<Order> orders = orderService.getAllOrders();

        assertThat(orders).extracting(Order::getId).containsExactly(first.getId(), second.getId());
    }

    @ParameterizedTest(name = "status={0}")
    @DisplayName("Restores the persisted order status without resetting it to CREATED")
    @EnumSource(OrderStatus.class)
    void shouldRestorePersistedStatus(OrderStatus status) {
        Order created = orderService.createOrder(validOrderBuilder().build());
        jdbcTemplate.update("UPDATE orders SET status = ? WHERE id = ?", status.name(), created.getId());
        entityManager.clear();

        Order persisted = orderService.getOrderById(created.getId());

        assertThat(persisted.getStatus()).isEqualTo(status);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @DisplayName("Persists an allowed status transition without replacing order positions")
    @MethodSource("validStatusTransitions")
    void shouldChangeStatusWithoutReplacingPositions(OrderStatus currentStatus, OrderStatus targetStatus) {
        Order created = orderService.createOrder(validOrderBuilder().build());
        List<Long> originalItemIds = findItemIds(created.getId());
        jdbcTemplate.update("UPDATE orders SET status = ? WHERE id = ?", currentStatus.name(), created.getId());
        entityManager.clear();
        Order requested = restoreBuilder(created).status(targetStatus).build();

        Order updated = orderService.updateOrder(requested);
        entityManager.clear();
        Order persisted = orderService.getOrderById(created.getId());
        List<Long> persistedItemIds = findItemIds(created.getId());

        assertThat(updated.getStatus()).isEqualTo(targetStatus);
        assertThat(persisted.getStatus()).isEqualTo(targetStatus);
        assertThat(persistedItemIds).containsExactlyElementsOf(originalItemIds);
    }

    @Test
    @DisplayName("Accepts an unchanged order status without modifying purchase details")
    void shouldAcceptUnchangedStatus() {
        Order created = orderService.createOrder(validOrderBuilder().build());

        Order updated = orderService.updateOrder(created);

        assertThat(updated).usingRecursiveComparison().isEqualTo(created);
    }

    @Test
    @DisplayName("Rejects skipping the payment step when changing order status")
    void shouldRejectSkippingPayment() {
        Order created = orderService.createOrder(validOrderBuilder().build());
        Order requested = restoreBuilder(created).status(OrderStatus.SHIPPED).build();

        Throwable exception = catchThrowable(() -> orderService.updateOrder(requested));

        assertThat(exception).isInstanceOf(InvalidOrderStateTransitionException.class);
    }

    @Test
    @DisplayName("Rejects reverting a paid order to CREATED")
    void shouldRejectRevertingStatusToCreated() {
        Order created = orderService.createOrder(validOrderBuilder().build());
        created.markAsPaid();
        Order paid = orderService.updateOrder(created);
        Order requested = restoreBuilder(paid).status(OrderStatus.CREATED).build();

        Throwable exception = catchThrowable(() -> orderService.updateOrder(requested));

        assertThat(exception).isInstanceOf(OrderConflictException.class);
    }

    @Test
    @DisplayName("Rejects changing the buyer of an existing order")
    void shouldRejectChangingBuyer() {
        Order created = orderService.createOrder(validOrderBuilder().build());
        Order requested = restoreBuilder(created).buyerId(OTHER_BUYER_ID).build();

        Throwable exception = catchThrowable(() -> orderService.updateOrder(requested));

        assertThat(exception).isInstanceOf(OrderConflictException.class);
    }

    @Test
    @DisplayName("Rejects changing the creation time of an existing order")
    void shouldRejectChangingCreationTime() {
        Order created = orderService.createOrder(validOrderBuilder().build());
        Order requested = restoreBuilder(created).createdAt(CREATED_AT.plusSeconds(1)).build();

        Throwable exception = catchThrowable(() -> orderService.updateOrder(requested));

        assertThat(exception).isInstanceOf(OrderConflictException.class);
    }

    @Test
    @DisplayName("Rejects replacing purchase snapshots in an existing order")
    void shouldRejectChangingPurchaseSnapshots() {
        Order created = orderService.createOrder(validOrderBuilder().build());
        Order requested = restoreBuilder(created).items(List.of(
                orderItem(PRODUCT_ID, "Changed product", "20", 2)
        )).build();

        Throwable exception = catchThrowable(() -> orderService.updateOrder(requested));

        assertThat(exception).isInstanceOf(OrderConflictException.class);
    }

    @ParameterizedTest(name = "status={0}")
    @DisplayName("Deletes a created or cancelled order together with all positions")
    @EnumSource(value = OrderStatus.class, names = {"CREATED", "CANCELLED"})
    void shouldDeleteOrderAndPositions(OrderStatus status) {
        Order created = orderService.createOrder(validOrderBuilder().build());
        if (status == OrderStatus.CANCELLED) {
            created.cancel();
            orderService.updateOrder(created);
        }

        orderService.deleteOrder(created.getId());
        entityManager.clear();
        var persistedOrder = orderRepository.findById(created.getId());
        Long itemCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM order_items WHERE order_id = ?", Long.class, created.getId()
        );

        assertThat(persistedOrder).isEmpty();
        assertThat(itemCount).isZero();
    }

    @Test
    @DisplayName("Rejects deleting a paid order")
    void shouldRejectDeletingPaidOrder() {
        Order created = orderService.createOrder(validOrderBuilder().build());
        created.markAsPaid();
        Order paid = orderService.updateOrder(created);

        Throwable exception = catchThrowable(() -> orderService.deleteOrder(paid.getId()));

        assertThat(exception).isInstanceOf(OrderConflictException.class);
    }

    @Test
    @DisplayName("Rejects creating an order with an existing ID")
    void shouldRejectDuplicateOrderId() {
        Order created = orderService.createOrder(validOrderBuilder().build());

        Throwable exception = catchThrowable(() -> orderService.createOrder(created));

        assertThat(exception).isInstanceOf(OrderConflictException.class);
    }

    @Test
    @DisplayName("Rejects creating an order that is already paid")
    void shouldRejectCreatingPaidOrder() {
        Order order = validOrderBuilder().build();
        order.markAsPaid();

        Throwable exception = catchThrowable(() -> orderService.createOrder(order));

        assertThat(exception).isInstanceOf(OrderConflictException.class);
    }

    @Test
    @DisplayName("Throws OrderNotFoundException when reading an unknown order")
    void shouldThrowWhenOrderIsNotFoundById() {
        Order missing = validOrderBuilder().build();

        Throwable exception = catchThrowable(() -> orderService.getOrderById(missing.getId()));

        assertThat(exception).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Throws OrderNotFoundException when updating an unknown order")
    void shouldThrowWhenOrderIsNotFoundForUpdate() {
        Order missing = validOrderBuilder().build();

        Throwable exception = catchThrowable(() -> orderService.updateOrder(missing));

        assertThat(exception).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Throws OrderNotFoundException when deleting an unknown order")
    void shouldThrowWhenOrderIsNotFoundForDeletion() {
        Order missing = validOrderBuilder().build();

        Throwable exception = catchThrowable(() -> orderService.deleteOrder(missing.getId()));

        assertThat(exception).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    @DisplayName("Does not insert an unknown order through repository status update")
    void shouldNotInsertUnknownOrderOnStatusUpdate() {
        Order missing = validOrderBuilder().build();

        boolean updated = orderRepository.updateStatus(missing.getId(), OrderStatus.PAID);

        assertThat(updated).isFalse();
        assertThat(orderRepository.findById(missing.getId())).isEmpty();
    }

    @Test
    @DisplayName("Returns false when the repository cannot find an order to delete")
    void shouldReturnFalseWhenDeletingUnknownOrder() {
        Order missing = validOrderBuilder().build();

        boolean deleted = orderRepository.deleteById(missing.getId());

        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("Rolls back the order and all positions when a product reference is invalid")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollBackWholeOrderWhenProductIsMissing() {
        Order invalid = validOrderBuilder().items(List.of(
                orderItem(PRODUCT_ID, "Valid item", "10", 1),
                orderItem(MISSING_PRODUCT_ID, "Missing product", "5", 1)
        )).build();

        Throwable exception = catchThrowable(() -> orderService.createOrder(invalid));
        Long orderCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM orders WHERE id = ?", Long.class, invalid.getId()
        );
        Long itemCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM order_items WHERE order_id = ?", Long.class, invalid.getId()
        );

        assertThat(exception).isInstanceOf(OrderConflictException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        assertThat(orderCount).isZero();
        assertThat(itemCount).isZero();
    }

    private List<Long> findItemIds(UUID orderId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM order_items WHERE order_id = ? ORDER BY position", Long.class, orderId
        );
    }

    private static Order.OrderBuilder validOrderBuilder() {
        return Order.builder()
                .buyerId(BUYER_ID)
                .createdAt(CREATED_AT)
                .items(List.of(orderItem(PRODUCT_ID, "Product snapshot", "10", 1)));
    }

    private static Order.RestoreBuilder restoreBuilder(Order source) {
        return Order.restoreBuilder()
                .id(source.getId())
                .buyerId(source.getBuyerId())
                .createdAt(source.getCreatedAt())
                .items(source.getItems())
                .status(source.getStatus());
    }

    private static OrderItem orderItem(UUID productId, String name, String amount, int quantity) {
        return OrderItem.builder()
                .productId(productId)
                .productName(name)
                .unitPrice(new Money(EUR, new BigDecimal(amount)))
                .quantity(new Quantity(quantity))
                .build();
    }

    private static Stream<Arguments> validStatusTransitions() {
        return Stream.of(
                Arguments.of(OrderStatus.CREATED, OrderStatus.PAID),
                Arguments.of(OrderStatus.PAID, OrderStatus.SHIPPED),
                Arguments.of(OrderStatus.SHIPPED, OrderStatus.DELIVERED)
        );
    }
}
