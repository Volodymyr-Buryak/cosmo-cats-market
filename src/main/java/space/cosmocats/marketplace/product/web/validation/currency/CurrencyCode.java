package space.cosmocats.marketplace.product.web.validation.currency;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;

import jakarta.validation.Payload;
import jakarta.validation.Constraint;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = CurrencyCodeValidator.class)
@Documented
public @interface CurrencyCode {
    String message() default "{validation.currency-code}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
