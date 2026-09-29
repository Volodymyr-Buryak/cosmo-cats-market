package space.cosmocats.marketplace.infrastructure.web.error.validation;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolation;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.BindingResult;
import space.cosmocats.marketplace.infrastructure.web.error.validation.ValidationProblemDetail.ValidationErrorDetails;

@Component
public class ValidationErrorMapper {

    private static final Set<String> PUBLIC_ARGUMENTS = Set.of("min", "max", "value", "inclusive", "regexp");

    List<ValidationErrorDetails> map(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(this::toDetails)
                .sorted(
                        Comparator.comparing(ValidationErrorDetails::target)
                                .thenComparing(ValidationErrorDetails::code)
                )
                .toList();
    }

    private ValidationErrorDetails toDetails(ObjectError error) {
        Map<String, Object> arguments = extractArguments(error);

        if (error instanceof FieldError fieldError) {
            return ValidationErrorDetails.field(error.getDefaultMessage(), fieldError.getField(), arguments);
        }

        return ValidationErrorDetails.object(error.getDefaultMessage(), error.getObjectName(), arguments);
    }

    private Map<String, Object> extractArguments(ObjectError error) {
        if (!error.contains(ConstraintViolation.class)) {
            return Map.of();
        }

        ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);

        return violation.getConstraintDescriptor()
                .getAttributes()
                .entrySet()
                .stream()
                .filter(entry -> PUBLIC_ARGUMENTS.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, ignored) -> existing,
                        LinkedHashMap::new
                ));
    }
}