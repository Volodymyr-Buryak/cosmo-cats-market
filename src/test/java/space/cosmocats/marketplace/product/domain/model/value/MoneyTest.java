package space.cosmocats.marketplace.product.domain.model.value;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    @ParameterizedTest(name = "[{index}] amount = {0}")
    @DisplayName("Accepts a non-negative amount without rounding and preserves the currency")
    @ValueSource(strings = {"0", "12.50", "1.234"})
    void shouldAcceptNonNegativeAmountWithoutRounding(String amount) {
        // Given
        BigDecimal expectedAmount = new BigDecimal(amount);

        // When
        Money money = new Money(USD, expectedAmount);

        // Then
        assertThat(money.currency()).isEqualTo(USD);
        assertThat(money.amount()).isEqualByComparingTo(expectedAmount);
    }

    @ParameterizedTest(name = "[{index}] amount = {0}")
    @DisplayName("Rejects a negative amount")
    @ValueSource(strings = {"-0.001", "-1", "-100"})
    void shouldRejectNegativeAmount(String amount) {
        BigDecimal negativeAmount = new BigDecimal(amount);

        Throwable exception = catchThrowable(() -> new Money(USD, negativeAmount));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "[{index}] {0} normalizes to {1}")
    @DisplayName("Normalizes trailing zeros and represents whole amounts without a negative scale")
    @CsvSource({"10.5000, 10.5", "1000.00, 1000", "0.00, 0", "0E+3, 0"})
    void shouldNormalizeAmount(String amount, String expectedAmount) {
        BigDecimal originalAmount = new BigDecimal(amount);

        Money money = new Money(USD, originalAmount);

        assertThat(money.amount()).isEqualTo(new BigDecimal(expectedAmount));
    }

    @Test
    @DisplayName("Rejects a null currency")
    void shouldRejectNullCurrency() {
        BigDecimal amount = BigDecimal.ONE;

        Throwable exception = catchThrowable(() -> new Money(null, amount));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects a null amount")
    void shouldRejectNullAmount() {
        Throwable exception = catchThrowable(() -> new Money(USD, null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "[{index}] currency = {0}")
    @DisplayName("Creates zero money in the requested currency")
    @ValueSource(strings = {"USD", "EUR", "JPY"})
    void shouldCreateZeroInRequestedCurrency(String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);

        Money money = Money.zero(currency);

        assertThat(money.currency()).isEqualTo(currency);
        assertThat(money.amount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Rejects creating zero money with a null currency")
    void shouldRejectZeroWithNullCurrency() {
        Throwable exception = catchThrowable(() -> Money.zero(null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "[{index}] amount = {0}, zero = {1}")
    @DisplayName("Distinguishes zero from a positive amount")
    @CsvSource({"0, true", "0.00, true", "0.01, false", "10, false"})
    void shouldIdentifyZeroAmount(String amount, boolean expectedZero) {
        Money money = money(amount);

        boolean zero = money.isZero();

        assertThat(zero).isEqualTo(expectedZero);
    }

    @Test
    @DisplayName("Treats equal amounts with different trailing zeros as equal money values")
    void shouldTreatAmountsWithDifferentTrailingZerosAsEqual() {
        Money first = money("10.0");
        Money second = money("10.00");

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
    }

    @Test
    @DisplayName("Treats money in different currencies as different values")
    void shouldDistinguishDifferentCurrencies() {
        Money dollars = new Money(USD, BigDecimal.TEN);
        Money euros = new Money(EUR, BigDecimal.TEN);

        assertThat(dollars).isNotEqualTo(euros);
    }

    @ParameterizedTest(name = "[{index}] {0} + {1} = {2}")
    @DisplayName("Adds money in the same currency without changing the operands")
    @CsvSource({"10.25, 2.75, 13", "10, 0, 10", "0, 2.5, 2.5"})
    void shouldAddMoneyWithoutChangingOperands(String amount, String otherAmount, String expectedAmount) {
        Money original = money(amount);
        Money other = money(otherAmount);

        Money result = original.add(other);

        assertThat(result).isEqualTo(money(expectedAmount));
        assertThat(original.amount()).isEqualByComparingTo(amount);
        assertThat(other.amount()).isEqualByComparingTo(otherAmount);
    }

    @Test
    @DisplayName("Rejects adding money in a different currency")
    void shouldRejectAddingMoneyInDifferentCurrency() {
        Money dollars = money("10");
        Money euros = new Money(EUR, BigDecimal.ONE);

        Throwable exception = catchThrowable(() -> dollars.add(euros));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "[{index}] {0} - {1} = {2}")
    @DisplayName("Subtracts money without changing the operands")
    @CsvSource({"10, 3.25, 6.75", "10, 10, 0", "10, 0, 10"})
    void shouldSubtractMoneyWithoutChangingOperands(String amount, String requestedAmount, String expectedAmount) {
        Money original = money(amount);
        Money requested = money(requestedAmount);

        Money result = original.subtract(requested);

        assertThat(result).isEqualTo(money(expectedAmount));
        assertThat(original.amount()).isEqualByComparingTo(amount);
        assertThat(requested.amount()).isEqualByComparingTo(requestedAmount);
    }

    @ParameterizedTest(name = "[{index}] available = {0}, requested = {1}")
    @DisplayName("Rejects subtracting more money than is available")
    @CsvSource({"1, 2", "0, 0.01"})
    void shouldRejectSubtractingMoreThanAvailable(String amount, String requestedAmount) {
        Money original = money(amount);
        Money requested = money(requestedAmount);

        Throwable exception = catchThrowable(() -> original.subtract(requested));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(original.amount()).isEqualByComparingTo(amount);
    }

    @Test
    @DisplayName("Rejects subtracting money in a different currency")
    void shouldRejectSubtractingMoneyInDifferentCurrency() {
        Money dollars = money("10");
        Money euros = new Money(EUR, BigDecimal.ONE);

        Throwable exception = catchThrowable(() -> dollars.subtract(euros));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "[{index}] {0} * {1} = {2}")
    @DisplayName("Multiplies money by a quantity without changing the operands")
    @CsvSource({"2.50, 4, 10", "2.50, 0, 0", "2.50, 1, 2.5"})
    void shouldMultiplyByQuantityWithoutChangingOperands(String amount, int quantity, String expectedAmount) {
        Money original = money(amount);
        Quantity multiplier = new Quantity(quantity);

        Money result = original.multiply(multiplier);

        assertThat(result).isEqualTo(money(expectedAmount));
        assertThat(original.amount()).isEqualByComparingTo(amount);
        assertThat(multiplier.value()).isEqualTo(quantity);
    }

    @Test
    @DisplayName("Rejects adding null money")
    void shouldRejectAddingNull() {
        Money money = money("10");

        Throwable exception = catchThrowable(() -> money.add(null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects subtracting null money")
    void shouldRejectSubtractingNull() {
        Money money = money("10");

        Throwable exception = catchThrowable(() -> money.subtract(null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects multiplying money by a null quantity")
    void shouldRejectMultiplyingByNull() {
        Money money = money("10");

        Throwable exception = catchThrowable(() -> money.multiply(null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    private static Money money(String amount) {
        return new Money(USD, new BigDecimal(amount));
    }
}
