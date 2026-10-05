package space.cosmocats.marketplace.common.web.error.handler;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.core.annotation.Order;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.WebRequest;
import space.cosmocats.marketplace.common.web.error.model.ApiError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import space.cosmocats.marketplace.common.web.error.model.ApiProblemFactory;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CustomResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private final ApiProblemFactory apiProblemFactory;

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<ApiError> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(this::toApiError)
                .toList();

        log.debug("Validation failed, request={}, errors={}", request.getDescription(false), errors);

        return handleExceptionInternal(
                ex,
                apiProblemFactory.validationError(ex, errors, request),
                headers,
                status,
                request
        );
    }

    private String toJsonPointer(String field) {
        return "/" + field
                .replace(".", "/")
                .replaceAll("\\[(\\d+)\\]", "/$1");
    }

    private ApiError toApiError(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            return new ApiError(fieldError.getDefaultMessage(), toJsonPointer(fieldError.getField()));
        }
        return new ApiError(error.getDefaultMessage(), null);
    }
}
