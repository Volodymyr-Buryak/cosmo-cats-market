package space.cosmocats.marketplace.domain.model.value;

import lombok.NonNull;
import java.util.Currency;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(
        @NonNull Currency currency,
        @NonNull BigDecimal amount
) {
    public Money {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException(String.format("Money amount must not be negative: %s", amount));
        }
        amount = normalize(amount);
    }

    public static Money zero(Currency currency) {
        return new Money(currency, BigDecimal.ZERO);
    }

    public Money add(@NonNull Money other) {
        checkCurrency(other);
        return new Money(currency, amount.add(other.amount));
    }

    public Money subtract(@NonNull Money other) {
        checkCurrency(other);
        if (amount.compareTo(other.amount) < 0) {
            throw new IllegalArgumentException(
                    String.format("Cannot subtract %s from money amount %s", other.amount, amount)
            );
        }
        return new Money(currency, amount.subtract(other.amount));
    }

    public Money multiply(@NonNull Quantity quantity) {
        return new Money(currency, amount.multiply(BigDecimal.valueOf(quantity.value())));
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void checkCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    String.format("Money currencies must match: %s != %s", currency, other.currency)
            );
        }
    }

    private static BigDecimal normalize(BigDecimal amount) {
        if (amount.signum() == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal normalizedAmount = amount.stripTrailingZeros();
        if (normalizedAmount.scale() < 0) {
            return normalizedAmount.setScale(0, RoundingMode.UNNECESSARY);
        }

        return normalizedAmount;
    }
}