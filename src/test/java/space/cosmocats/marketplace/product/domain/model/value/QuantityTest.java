package space.cosmocats.marketplace.product.domain.model.value;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class QuantityTest {

    @ParameterizedTest(name = "[{index}] value = {0}")
    @DisplayName("Accepts zero, positive values and the maximum integer value")
    @ValueSource(ints = {0, 1, Integer.MAX_VALUE})
    void shouldAcceptNonNegativeValue(int value) {
        // Given
        int expectedValue = value;

        // When
        Quantity quantity = new Quantity(value);

        // Then
        assertThat(quantity.value()).isEqualTo(expectedValue);
    }

    @ParameterizedTest(name = "[{index}] value = {0}")
    @DisplayName("Rejects negative values")
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void shouldRejectNegativeValue(int value) {
        Throwable exception = catchThrowable(() -> new Quantity(value));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Exposes ZERO as a quantity with value zero")
    void shouldProvideZeroConstant() {
        assertThat(Quantity.ZERO).isEqualTo(new Quantity(0));
    }

    @ParameterizedTest(name = "[{index}] value = {0}, zero = {1}")
    @DisplayName("Distinguishes zero from a positive quantity")
    @CsvSource({"0, true", "1, false", "2147483647, false"})
    void shouldIdentifyZeroQuantity(int value, boolean expectedZero) {
        Quantity quantity = new Quantity(value);

        boolean zero = quantity.isZero();

        assertThat(zero).isEqualTo(expectedZero);
    }

    @ParameterizedTest(name = "[{index}] {0} + {1} = {2}")
    @DisplayName("Adds quantities without changing the operands")
    @CsvSource({"2, 3, 5", "5, 0, 5", "0, 5, 5", "2147483646, 1, 2147483647"})
    void shouldAddQuantitiesWithoutChangingOperands(int value, int otherValue, int expectedValue) {
        Quantity original = new Quantity(value);
        Quantity other = new Quantity(otherValue);

        Quantity result = original.add(other);

        assertThat(result).isEqualTo(new Quantity(expectedValue));
        assertThat(original.value()).isEqualTo(value);
        assertThat(other.value()).isEqualTo(otherValue);
    }

    @ParameterizedTest(name = "[{index}] {0} + {1} overflows")
    @DisplayName("Rejects addition that exceeds the maximum integer value")
    @CsvSource({"2147483647, 1", "2147483647, 2147483647"})
    void shouldRejectAdditionOverflow(int value, int otherValue) {
        Quantity original = new Quantity(value);
        Quantity other = new Quantity(otherValue);

        Throwable exception = catchThrowable(() -> original.add(other));

        assertThat(exception).isExactlyInstanceOf(ArithmeticException.class);
        assertThat(original.value()).isEqualTo(value);
    }

    @ParameterizedTest(name = "[{index}] {0} - {1} = {2}")
    @DisplayName("Subtracts quantities without changing the operands")
    @CsvSource({"5, 2, 3", "5, 5, 0", "5, 0, 5", "0, 0, 0"})
    void shouldSubtractQuantitiesWithoutChangingOperands(int value, int requestedValue, int expectedValue) {
        Quantity original = new Quantity(value);
        Quantity requested = new Quantity(requestedValue);

        Quantity result = original.subtract(requested);

        assertThat(result).isEqualTo(new Quantity(expectedValue));
        assertThat(original.value()).isEqualTo(value);
        assertThat(requested.value()).isEqualTo(requestedValue);
    }

    @ParameterizedTest(name = "[{index}] available = {0}, requested = {1}")
    @DisplayName("Rejects subtracting more than the available quantity")
    @CsvSource({"2, 3", "0, 1"})
    void shouldRejectSubtractingMoreThanAvailable(int value, int requestedValue) {
        Quantity original = new Quantity(value);
        Quantity requested = new Quantity(requestedValue);

        Throwable exception = catchThrowable(() -> original.subtract(requested));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(original.value()).isEqualTo(value);
    }

    @Test
    @DisplayName("Rejects adding a null quantity")
    void shouldRejectAddingNull() {
        Quantity quantity = new Quantity(5);

        Throwable exception = catchThrowable(() -> quantity.add(null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects subtracting a null quantity")
    void shouldRejectSubtractingNull() {
        Quantity quantity = new Quantity(5);

        Throwable exception = catchThrowable(() -> quantity.subtract(null));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
    }
}
