package space.cosmocats.marketplace.order.application.service.impl;

import java.util.Optional;
import java.util.UUID;
import java.util.Currency;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import space.cosmocats.marketplace.order.application.command.CreateOrderCommand;
import space.cosmocats.marketplace.order.domain.repository.OrderRepository;
import space.cosmocats.marketplace.order.domain.model.Order;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.product.application.service.ProductService;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Mock private OrderRepository orderRepository;
    @Mock private ProductService productService;
    @InjectMocks private OrderServiceImpl orderService;

    @Test
    @DisplayName("Creates order lines from the current catalog snapshot")
    void shouldCreateOrderFromCommand() {
        Product product = Product.builder().id(ID).name("Blue Star").description("Equipment for space travel")
                .price(new Money(Currency.getInstance("USD"), BigDecimal.TEN)).stock(new Quantity(10)).categoryId(ID).build();
        when(productService.getProductById(ID)).thenReturn(product);
        when(orderRepository.create(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var command = new CreateOrderCommand(ID, java.util.List.of(new CreateOrderCommand.Item(ID, 2)));

        Order result = orderService.createOrder(command);

        assertThat(result.getBuyerId()).isEqualTo(ID);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().productName()).isEqualTo("Blue Star");
        assertThat(result.getItems().getFirst().unitPrice()).isEqualTo(product.getPrice());
        assertThat(result.getItems().getFirst().quantity()).isEqualTo(new Quantity(2));
        verify(orderRepository).create(result);
    }

    @Test
    @DisplayName("Updates order status while retaining purchase details")
    void shouldUpdateStatusWithoutChangingPurchaseDetails() {
        Order stored = Order.builder().id(ID).buyerId(ID).createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .items(java.util.List.of(new space.cosmocats.marketplace.order.domain.model.value.OrderItem(
                        ID, "Blue Star", new Money(Currency.getInstance("USD"), BigDecimal.TEN), new Quantity(2))))
                .build();
        when(orderRepository.findByIdForUpdate(ID)).thenReturn(Optional.of(stored));
        when(orderRepository.updateStatus(ID, OrderStatus.PAID)).thenReturn(true);

        Order result = orderService.updateStatus(ID, OrderStatus.PAID);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(result.getBuyerId()).isEqualTo(stored.getBuyerId());
        assertThat(result.getCreatedAt()).isEqualTo(stored.getCreatedAt());
        assertThat(result.getItems()).isEqualTo(stored.getItems());
        verify(orderRepository).findByIdForUpdate(ID);
        verify(orderRepository).updateStatus(ID, OrderStatus.PAID);
    }
}
