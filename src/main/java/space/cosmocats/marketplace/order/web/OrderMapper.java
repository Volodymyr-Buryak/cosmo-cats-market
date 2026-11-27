package space.cosmocats.marketplace.order.web;

import org.mapstruct.Mapper;
import space.cosmocats.marketplace.order.application.command.CreateOrderCommand;
import space.cosmocats.marketplace.order.web.dto.OrderRequest;
import space.cosmocats.marketplace.order.web.dto.OrderItemRequest;
import org.mapstruct.Mapping;
import space.cosmocats.marketplace.order.domain.model.Order;
import space.cosmocats.marketplace.order.domain.model.value.OrderItem;
import space.cosmocats.marketplace.order.web.dto.OrderResponse;
import space.cosmocats.marketplace.order.web.dto.OrderItemResponse;

@Mapper
public interface OrderMapper {
    CreateOrderCommand toCommand(OrderRequest request);
    CreateOrderCommand.Item toCommand(OrderItemRequest request);

    @Mapping(target = "total", source = "total.amount")
    @Mapping(target = "currency", source = "total.currency.currencyCode")
    OrderResponse toResponse(Order order);

    @Mapping(target = "unitPrice", source = "unitPrice.amount")
    @Mapping(target = "currency", source = "unitPrice.currency.currencyCode")
    @Mapping(target = "quantity", source = "quantity.value")
    OrderItemResponse toResponse(OrderItem item);
}
