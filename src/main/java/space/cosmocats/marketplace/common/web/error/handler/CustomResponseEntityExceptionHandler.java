package space.cosmocats.marketplace.common.web.error.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.core.annotation.Order;
import org.springframework.web.context.request.WebRequest;
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
        log.debug(
                "Validation failed, request={}, errors={}",
                request.getDescription(false), ex.getBindingResult().getAllErrors()
        );

        return handleExceptionInternal(
                ex,
                apiProblemFactory.validationError(ex, request),
                headers,
                status,
                request
        );
    }

}
