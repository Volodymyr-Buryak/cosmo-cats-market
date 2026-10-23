package space.cosmocats.marketplace.order.domain.model;

import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import java.util.Currency;
import java.util.ArrayList;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import space.cosmocats.marketplace.order.domain.model.value.OrderItem;
import space.cosmocats.marketplace.order.domain.exception.EmptyOrderException;
import space.cosmocats.marketplace.order.domain.exception.InvalidOrderStateTransitionException;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OrderTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T12:00:00Z");
    private static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BUYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @Test
    @DisplayName("Creates an order with the provided data and CREATED status")
    void shouldCreateOrderWithProvidedDataAndCreatedStatus() {
        // Given
        OrderItem item = orderItem(PRODUCT_ID, "12.50", 2);
        var builder = validOrderBuilder().id(ORDER_ID).items(List.of(item));

        // When
        Order order = builder.build();

        // Then
        assertThat(order.getId()).isEqualTo(ORDER_ID);
        assertThat(order.getBuyerId()).isEqualTo(BUYER_ID);
        assertThat(order.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(order.getItems()).containsExactly(item);
        assertThat(order.getTotal()).isEqualTo(new Money(USD, new BigDecimal("25.00")));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
    }

    @Test
    @DisplayName("Generates an order ID when none is provided")
    void shouldGenerateIdWhenNotProvided() {
        var builder = validOrderBuilder();

        Order order = builder.build();

        assertThat(order.getId()).isNotNull();
    }

    @ParameterizedTest(name = "[{index}] total = {4}")
    @DisplayName("Calculates the total from all item prices and quantities")
    @CsvSource({"10.25, 2, 3.75, 3, 31.75", "0.01, 3, 0.02, 2, 0.07"})
    void shouldCalculateTotalFromAllItems(
            String firstPrice, int firstQuantity,
            String secondPrice, int secondQuantity, String expectedTotal
    ) {
        List<OrderItem> items = List.of(
                orderItem(PRODUCT_ID, firstPrice, firstQuantity),
                orderItem(OTHER_PRODUCT_ID, secondPrice, secondQuantity)
        );
        var builder = validOrderBuilder().items(items);

        Order order = builder.build();

        assertThat(order.getTotal()).isEqualTo(new Money(USD, new BigDecimal(expectedTotal)));
    }

    @Test
    @DisplayName("Rejects creating an empty order")
    void shouldRejectEmptyOrder() {
        var builder = validOrderBuilder().items(List.of());

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isInstanceOf(EmptyOrderException.class);
    }

    @ParameterizedTest(name = "[{index}] missing field = {0}")
    @DisplayName("Rejects building an order without a required field")
    @MethodSource("buildersWithMissingRequiredFields")
    void shouldRejectBuildingOrderWithoutRequiredField(String fieldName, Order.OrderBuilder builder) {
        Throwable exception = catchThrowable(builder::build);

        assertThat(exception)
                .isExactlyInstanceOf(NullPointerException.class)
                .hasMessageContaining(fieldName);
    }

    @Test
    @DisplayName("Rejects a null element in the order items")
    void shouldRejectNullItem() {
        List<OrderItem> items = Arrays.asList(orderItem(PRODUCT_ID, "12.50", 2), null);
        var builder = validOrderBuilder().items(items);

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects order items with different currencies")
    void shouldRejectItemsWithDifferentCurrencies() {
        OrderItem dollarItem = orderItem(PRODUCT_ID, "10.00", 1);
        OrderItem euroItem = new OrderItem(
                OTHER_PRODUCT_ID, "Galaxy Lamp",
                new Money(Currency.getInstance("EUR"), BigDecimal.ONE), new Quantity(1)
        );
        var builder = validOrderBuilder().items(List.of(dollarItem, euroItem));

        Throwable exception = catchThrowable(builder::build);

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Copies the input items so later list changes do not affect the order")
    void shouldCopyInputItems() {
        OrderItem item = orderItem(PRODUCT_ID, "12.50", 2);
        List<OrderItem> items = new ArrayList<>(List.of(item));
        Order order = validOrderBuilder().items(items).build();

        items.clear();

        assertThat(order.getItems()).containsExactly(item);
        assertThat(order.getTotal()).isEqualTo(new Money(USD, new BigDecimal("25.00")));
    }

    @Test
    @DisplayName("Returns an unmodifiable list of order items")
    void shouldReturnUnmodifiableItems() {
        OrderItem item = orderItem(PRODUCT_ID, "12.50", 2);
        Order order = validOrderBuilder().items(List.of(item)).build();

        Throwable exception = catchThrowable(() -> order.getItems().clear());

        assertThat(exception).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThat(order.getItems()).containsExactly(item);
    }


    @Test
    @DisplayName("Marks a created order as paid")
    void shouldMarkCreatedOrderAsPaid() {
        Order order = orderInStatus(OrderStatus.CREATED);

        order.markAsPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @ParameterizedTest(name = "[{index}] current status = {0}")
    @DisplayName("Rejects payment from any status other than CREATED")
    @EnumSource(value = OrderStatus.class, names = "CREATED", mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectPaymentFromInvalidStatus(OrderStatus initialStatus) {
        Order order = orderInStatus(initialStatus);

        Throwable exception = catchThrowable(order::markAsPaid);

        assertThat(exception).isInstanceOf(InvalidOrderStateTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(initialStatus);
    }

    @Test
    @DisplayName("Marks a paid order as shipped")
    void shouldMarkPaidOrderAsShipped() {
        Order order = orderInStatus(OrderStatus.PAID);

        order.markAsShipped();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @ParameterizedTest(name = "[{index}] current status = {0}")
    @DisplayName("Rejects shipping from any status other than PAID")
    @EnumSource(value = OrderStatus.class, names = "PAID", mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectShippingFromInvalidStatus(OrderStatus initialStatus) {
        Order order = orderInStatus(initialStatus);

        Throwable exception = catchThrowable(order::markAsShipped);

        assertThat(exception).isInstanceOf(InvalidOrderStateTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(initialStatus);
    }


    @Test
    @DisplayName("Marks a shipped order as delivered")
    void shouldMarkShippedOrderAsDelivered() {
        Order order = orderInStatus(OrderStatus.SHIPPED);

        order.markAsDelivered();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @ParameterizedTest(name = "[{index}] current status = {0}")
    @DisplayName("Rejects delivery from any status other than SHIPPED")
    @EnumSource(value = OrderStatus.class, names = "SHIPPED", mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectDeliveryFromInvalidStatus(OrderStatus initialStatus) {
        Order order = orderInStatus(initialStatus);

        Throwable exception = catchThrowable(order::markAsDelivered);

        assertThat(exception).isInstanceOf(InvalidOrderStateTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(initialStatus);
    }

    @ParameterizedTest(name = "[{index}] current status = {0}")
    @DisplayName("Allows cancelling a created or paid order")
    @EnumSource(value = OrderStatus.class, names = {"CREATED", "PAID"})
    void shouldCancelOrderBeforeShipping(OrderStatus initialStatus) {
        Order order = orderInStatus(initialStatus);

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @ParameterizedTest(name = "[{index}] current status = {0}")
    @DisplayName("Rejects cancelling a shipped, delivered or already cancelled order")
    @EnumSource(value = OrderStatus.class, names = {"SHIPPED", "DELIVERED", "CANCELLED"})
    void shouldRejectCancellationFromInvalidStatus(OrderStatus initialStatus) {
        Order order = orderInStatus(initialStatus);

        Throwable exception = catchThrowable(order::cancel);

        assertThat(exception).isInstanceOf(InvalidOrderStateTransitionException.class);
        assertThat(order.getStatus()).isEqualTo(initialStatus);
    }

    @Test
    @DisplayName("Identifies orders by ID regardless of their other data")
    void shouldTreatOrdersWithSameIdAsEqual() {
        UUID otherBuyerId = UUID.fromString("00000000-0000-0000-0000-000000000005");
        Order firstOrder = validOrderBuilder().id(ORDER_ID).build();
        Order secondOrder = validOrderBuilder().id(ORDER_ID).buyerId(otherBuyerId).build();

        assertThat(firstOrder).isEqualTo(secondOrder).hasSameHashCodeAs(secondOrder);
    }

    @Test
    @DisplayName("Distinguishes orders with different IDs")
    void shouldDistinguishOrdersWithDifferentIds() {
        Order firstOrder = validOrderBuilder().id(ORDER_ID).build();
        Order secondOrder = validOrderBuilder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000005"))
                .build();

        assertThat(firstOrder).isNotEqualTo(secondOrder);
    }

    private static Stream<Arguments> buildersWithMissingRequiredFields() {
        List<OrderItem> items = List.of(orderItem(PRODUCT_ID, "12.50", 2));

        return Stream.of(
                Arguments.of("buyerId", Order.builder().items(items).createdAt(CREATED_AT)),
                Arguments.of("items", Order.builder().buyerId(BUYER_ID).createdAt(CREATED_AT)),
                Arguments.of("createdAt", Order.builder().buyerId(BUYER_ID).items(items))
        );
    }

    private static Order orderInStatus(OrderStatus status) {
        Order order = validOrderBuilder().build();
        switch (status) {
            case CREATED -> {}
            case PAID -> order.markAsPaid();
            case SHIPPED -> {
                order.markAsPaid();
                order.markAsShipped();
            }
            case DELIVERED -> {
                order.markAsPaid();
                order.markAsShipped();
                order.markAsDelivered();
            }
            case CANCELLED -> order.cancel();
        }
        return order;
    }

    private static Order.OrderBuilder validOrderBuilder() {
        return Order.builder()
                .buyerId(BUYER_ID)
                .items(List.of(orderItem(PRODUCT_ID, "12.50", 2)))
                .createdAt(CREATED_AT);
    }

    private static OrderItem orderItem(UUID productId, String unitPrice, int quantity) {
        return new OrderItem(
                productId, "Blue Star",
                new Money(USD, new BigDecimal(unitPrice)), new Quantity(quantity)
        );
    }
}
