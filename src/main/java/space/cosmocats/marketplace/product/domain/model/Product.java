package space.cosmocats.marketplace.product.domain.model;

import lombok.*;
import java.util.UUID;
import java.util.Objects;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.domain.model.value.CosmicWord;
import space.cosmocats.marketplace.product.domain.exception.InsufficientStockException;
import space.cosmocats.marketplace.product.domain.exception.ProductPriceMustBePositiveException;
import space.cosmocats.marketplace.product.domain.exception.ProductNameMustContainSpaceWordException;

@Value
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product {
    @EqualsAndHashCode.Include
    UUID id;

    @With String name;
    @With String description;
    @With Money price;
    @With UUID categoryId;

    @With(AccessLevel.PRIVATE)
    Quantity stock;

    @Builder
    private Product(
            UUID id,
            @NonNull String name,
            String description,
            @NonNull Money price,
            @NonNull UUID categoryId,
            @NonNull Quantity stock
    ) {
        this.id = Objects.requireNonNullElseGet(id, UUID::randomUUID);
        this.name = validateName(name);
        this.description = (description == null) ? "" : description.strip();
        this.price = validatePrice(price);
        this.categoryId = categoryId;
        this.stock = stock;
    }

    public Product removeStock(@NonNull Quantity amount) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("Stock amount must be greater than zero");
        }
        if (amount.value() > stock.value()) {
            throw new InsufficientStockException(id, stock.value(), amount.value());
        }
        return withStock(stock.subtract(amount));
    }

    private static String validateName(String name) {
        String normalized = name.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }
        if (!CosmicWord.occursIn(normalized)) {
            throw new ProductNameMustContainSpaceWordException();
        }
        return normalized;
    }

    private static Money validatePrice(Money price) {
        if (price.isZero()) {
            throw new ProductPriceMustBePositiveException();
        }
        return price;
    }

}