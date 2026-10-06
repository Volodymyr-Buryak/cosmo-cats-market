package space.cosmocats.marketplace.product.web.validation.cosmicword;

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
@Constraint(validatedBy = CosmicWordValidator.class)
@Documented
@Retention(RUNTIME)
public @interface CosmicWordCheck {
    String message() default "{validation.cosmic-word}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
