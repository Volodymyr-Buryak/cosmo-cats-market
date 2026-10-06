package space.cosmocats.marketplace.product.domain.model.value;

import lombok.NonNull;

public record Quantity(int value) {

    public static final Quantity ZERO = new Quantity(0);

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException(String.format("Quantity must not be negative: %d", value));
        }
    }

    public Quantity add(@NonNull Quantity other) {
        return new Quantity(Math.addExact(value, other.value));
    }

    public Quantity subtract(@NonNull Quantity requested) {
        if (requested.value > value) {
            throw new IllegalArgumentException(
                    String.format("Cannot subtract %d from quantity %d", requested.value, value)
            );
        }
        return new Quantity(value - requested.value);
    }

    public boolean isZero() {
        return value == 0;
    }
}
