package space.cosmocats.marketplace.infrastructure.web.error.validation;

import lombok.*;
import java.net.URI;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ProblemDetail;

@Getter
public final class ValidationProblemDetail extends ProblemDetail {

    private static final String CODE = "VALIDATION_ERROR";
    private static final URI TYPE = URI.create("https://cosmo-cats.market/errors/validation");

    private final String code = CODE;
    private final List<ValidationErrorDetails> invalidParameters;

    @Builder
    private ValidationProblemDetail(ProblemDetail problemDetail, List<ValidationErrorDetails> invalidParameters) {
        super(Objects.requireNonNull(problemDetail, "problemDetail must not be null"));
        this.invalidParameters = (invalidParameters == null) ? List.of() : List.copyOf(invalidParameters);
        setType(TYPE);
    }

    public record ValidationErrorDetails(
            String code,
            String target,
            ValidationErrorType type,
            Map<String, Object> arguments
    ) {
        private static final String DEFAULT_CODE = "INVALID_PARAMETER";

        public ValidationErrorDetails {
            code = (code == null) ? DEFAULT_CODE : code;
            type = (type == null) ? ValidationErrorType.OBJECT : type;
            arguments = (arguments == null) ? Map.of() : Map.copyOf(arguments);
        }

        public static ValidationErrorDetails field(String code, String field, Map<String, Object> arguments) {
            return new ValidationErrorDetails(code, field, ValidationErrorType.FIELD, arguments);
        }

        public static ValidationErrorDetails object(String code, String objectName, Map<String, Object> arguments) {
            return new ValidationErrorDetails(code, objectName, ValidationErrorType.OBJECT, arguments);
        }

        public enum ValidationErrorType {
            FIELD, OBJECT
        }
    }
}