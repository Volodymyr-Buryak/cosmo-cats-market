package space.cosmocats.marketplace.order.domain.model.value;

import lombok.*;
import java.util.UUID;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;

@Builder(toBuilder = true)
public record OrderItem(
        @NonNull UUID productId,
        @NonNull String productName,
        @NonNull Money unitPrice,
        @NonNull Quantity quantity
) {
    public OrderItem {
        productName = productName.strip();

        if (productName.isEmpty()) {
            throw new IllegalArgumentException("Order item product name must not be blank");
        }

        if (unitPrice.isZero()) {
            throw new IllegalArgumentException("Order item unit price must be greater than zero");
        }

        if (quantity.isZero()) {
            throw new IllegalArgumentException("Order item quantity must be greater than zero");
        }
    }

    public static OrderItem of(Product product, Quantity quantity) {
        return new OrderItem(product.getId(), product.getName(), product.getPrice(), quantity);
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}