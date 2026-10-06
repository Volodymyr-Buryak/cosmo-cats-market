package space.cosmocats.marketplace.product.web.validation.cosmicword;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import space.cosmocats.marketplace.product.domain.model.value.CosmicWord;

public class CosmicWordValidator implements ConstraintValidator<CosmicWordCheck, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || CosmicWord.occursIn(value);
    }
}
