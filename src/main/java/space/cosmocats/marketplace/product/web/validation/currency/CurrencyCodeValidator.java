package space.cosmocats.marketplace.product.web.validation.currency;

import java.util.Set;
import java.util.Currency;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CurrencyCodeValidator implements ConstraintValidator<CurrencyCode, String> {

    private static final Set<String> CODES = Currency.getAvailableCurrencies().stream()
            .map(Currency::getCurrencyCode)
            .collect(Collectors.toUnmodifiableSet());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || CODES.contains(value);
    }

}
