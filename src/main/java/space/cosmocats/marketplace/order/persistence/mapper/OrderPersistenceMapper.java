package space.cosmocats.marketplace.order.persistence.mapper;

import org.mapstruct.*;
import java.util.Currency;
import space.cosmocats.marketplace.order.domain.model.Order;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.order.domain.model.value.OrderItem;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.order.persistence.entity.OrderEntity;
import space.cosmocats.marketplace.order.persistence.entity.OrderItemEntity;

@Mapper(imports = {Money.class, Quantity.class, Currency.class})
public interface OrderPersistenceMapper {
    @Mapping(target = "currency", source = "total.currency.currencyCode")
    OrderEntity toEntity(Order order);

    @Mapping(target = "order", source = "order")
    @Mapping(target = "position", source = "position")
    @Mapping(target = "productId", source = "item.productId")
    @Mapping(target = "productName", source = "item.productName")
    @Mapping(target = "unitPriceAmount", source = "item.unitPrice.amount")
    @Mapping(target = "quantity", source = "item.quantity.value")
    OrderItemEntity toItemEntity(OrderItem item, OrderEntity order, int position);

    @Mapping(target = "unitPrice", expression = "java(new Money(Currency.getInstance(currency), entity.getUnitPriceAmount()))")
    @Mapping(target = "quantity", expression = "java(new Quantity(entity.getQuantity()))")
    OrderItem toItemDomain(OrderItemEntity entity, @Context String currency);

    default Order toDomain(OrderEntity entity) {
        if (entity == null) {
            return null;
        }
        return Order.restoreBuilder()
                .id(entity.getId())
                .buyerId(entity.getBuyerId())
                .createdAt(entity.getCreatedAt())
                .status(entity.getStatus())
                .items(
                        entity.getItems().stream()
                                .map(item -> toItemDomain(item, entity.getCurrency()))
                                .toList()
                )
                .build();
    }
}
