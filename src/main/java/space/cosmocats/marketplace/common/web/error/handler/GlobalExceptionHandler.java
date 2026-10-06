package space.cosmocats.marketplace.common.web.error.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import space.cosmocats.marketplace.common.web.error.model.ApiProblem;
import space.cosmocats.marketplace.common.exception.ConflictException;
import space.cosmocats.marketplace.common.exception.NotFoundException;
import space.cosmocats.marketplace.common.web.error.model.ApiProblemFactory;
import space.cosmocats.marketplace.common.exception.DomainRuleViolationException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApiProblemFactory problemFactory;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiProblem> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Unhandled exception, request={}", request.getDescription(false), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(problemFactory.internalServerError(request));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiProblem> handleNotFoundException(NotFoundException ex, WebRequest request) {
        log.debug("Resource not found: {}, request={}", ex.getMessage(), request.getDescription(false));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemFactory.notFound(ex, request));
    }

    @ExceptionHandler(DomainRuleViolationException.class)
    public ResponseEntity<ApiProblem> handleDomainRuleViolationException(
            DomainRuleViolationException ex, WebRequest request
    ) {
        log.info("Domain rule violation: {}, request={}", ex.getMessage(), request.getDescription(false));

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(problemFactory.domainRuleViolation(ex, request));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiProblem> handleConflictException(ConflictException ex, WebRequest request) {
        log.info("Request conflict: {}, request={}", ex.getMessage(), request.getDescription(false));

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(problemFactory.conflict(ex, request));
    }

}
