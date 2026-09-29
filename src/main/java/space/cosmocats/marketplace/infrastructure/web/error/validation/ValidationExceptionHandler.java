package space.cosmocats.marketplace.infrastructure.web.error.validation;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ValidationExceptionHandler extends ResponseEntityExceptionHandler {

    private final ValidationErrorMapper validationErrorMapper;

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request
    ) {
        BindingResult bindingResult = exception.getBindingResult();
        log.info("Request body validation failed: {} violation(s)", bindingResult.getErrorCount());

        var problem = ProblemDetail.forStatusAndDetail(status, "The request contains invalid parameters");
        problem.setTitle("Validation failed");

        if (request instanceof ServletWebRequest servletRequest) {
            problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
        }

        return ResponseEntity.status(status)
                .headers(headers)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(
                        ValidationProblemDetail.builder()
                                .problemDetail(problem)
                                .invalidParameters(validationErrorMapper.map(bindingResult))
                                .build()
                );
    }
}
