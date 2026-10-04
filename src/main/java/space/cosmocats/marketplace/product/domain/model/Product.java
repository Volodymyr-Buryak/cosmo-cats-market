package space.cosmocats.marketplace.product.domain.model;

import lombok.*;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import java.util.Arrays;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.domain.exception.InsufficientStockException;
import space.cosmocats.marketplace.product.domain.exception.ProductPriceMustBePositiveException;
import space.cosmocats.marketplace.product.domain.exception.ProductNameMustContainSpaceWordException;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Product {

    private static final Set<String> SPACE_WORDS = Set.of("star", "galaxy", "comet");

    @EqualsAndHashCode.Include
    private final UUID id;
    private final String name;
    private final String description;
    private final Money price;
    private final UUID categoryId;
    private Quantity stock;

    @Builder
    private Product(
            @NonNull UUID id,
            @NonNull String name,
            String description,
            @NonNull Money price,
            @NonNull Quantity stock,
            @NonNull UUID categoryId
    ) {
        String cleanName = name.strip();

        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Product name must not be blank");
        }

        if (!hasSpaceWord(cleanName)) {
            throw new ProductNameMustContainSpaceWordException();
        }

        if (price.isZero()) {
            throw new ProductPriceMustBePositiveException();
        }

        this.id = id;
        this.name = cleanName;
        this.description = (description == null) ? "" : description.strip();
        this.price = price;
        this.stock = stock;
        this.categoryId = categoryId;
    }

    public void removeStock(@NonNull Quantity amount) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("Stock amount must be greater than zero");
        }
        if (amount.value() > stock.value()) {
            throw new InsufficientStockException(id, stock.value(), amount.value());
        }
        stock = stock.subtract(amount);
    }

    private static boolean hasSpaceWord(String name) {
        return Arrays.stream(name.toLowerCase(Locale.ROOT).split("[^a-z]+"))
                .anyMatch(SPACE_WORDS::contains);
    }
}
