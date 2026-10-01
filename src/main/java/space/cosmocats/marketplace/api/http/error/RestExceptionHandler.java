package space.cosmocats.marketplace.api.http.error;

import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.request.WebRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import space.cosmocats.marketplace.api.http.error.dto.ApiError;
import space.cosmocats.marketplace.api.http.error.dto.ApiProblem;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String BASE_ERROR_URI = "https://cosmo-cats.market/errors";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request
    ) {
        ProblemDetail problemDetail = ex.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        problemDetail.setType(URI.create(BASE_ERROR_URI + "/validation-error"));

        List<ApiError> apiErrors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(this::toApiError)
                .toList();

        log.warn(
                "Validation failed for request: {}, errors: {}",
                request.getDescription(false), apiErrors
        );
        return handleExceptionInternal(ex, new ApiProblem(problemDetail, apiErrors), headers, status, request);
    }

    private ApiError toApiError(ObjectError err) {
        if (err instanceof FieldError fe) {
            return new ApiError(
                    fe.getDefaultMessage(),
                    "/%s".formatted(fe.getField()
                            .replace(".", "/")
                            .replaceAll("\\[(\\d+)]", "/$1")
                    )
            );
        }
        return new ApiError(err.getDefaultMessage(), null);
    }

}
