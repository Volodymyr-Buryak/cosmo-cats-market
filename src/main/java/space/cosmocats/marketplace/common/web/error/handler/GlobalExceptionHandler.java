package space.cosmocats.marketplace.common.web.error.handler;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import space.cosmocats.marketplace.common.web.error.model.ApiProblem;
import space.cosmocats.marketplace.common.exception.NotFoundException;
import space.cosmocats.marketplace.common.exception.BusinessRuleException;
import space.cosmocats.marketplace.common.web.error.model.ApiProblemFactory;

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
        log.debug("{}, request={}", ex.getMessage(), request.getDescription(false));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(problemFactory.notFound(ex, request));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiProblem> handleBusinessRuleException(BusinessRuleException ex, WebRequest request) {
        log.warn("{}, request={}", ex.getMessage(), request.getDescription(false));

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(problemFactory.businessRuleViolation(ex, request));
    }

}
