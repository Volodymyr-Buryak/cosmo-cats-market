package space.cosmocats.marketplace.common.web.error.model;

import java.net.URI;
import java.util.List;
import org.springframework.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.context.MessageSource;
import org.springframework.web.context.request.WebRequest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import space.cosmocats.marketplace.common.exception.ConflictException;
import space.cosmocats.marketplace.common.exception.NotFoundException;
import space.cosmocats.marketplace.common.exception.DomainRuleViolationException;

@Component
@RequiredArgsConstructor
public class ApiProblemFactory {
    private static final String BASE_ERROR_URI = "https://cosmo-cats.market/errors";

    private final MessageSource messageSource;

    public ApiProblem notFound(NotFoundException exception, WebRequest request) {
        return ApiProblem.builder()
                .status(HttpStatus.NOT_FOUND)
                .type(errorType("not-found"))
                .title(localize("error.not-found.title", "Not Found"))
                .detail(localize(
                        exception.getMessageKey(),
                        "Requested resource was not found.", exception.getArgs()
                ))
                .instance(resolveRequestInstance(request))
                .build();
    }

    public ApiProblem domainRuleViolation(DomainRuleViolationException exception, WebRequest request) {
        return ApiProblem.builder()
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .type(errorType("domain-rule-violation"))
                .title(localize("error.domain-rule-violation.title", "Domain rule violation"))
                .detail(localize(
                        exception.getMessageKey(),
                        "The operation could not be completed because it violates a domain rule.",
                        exception.getArgs()
                ))
                .instance(resolveRequestInstance(request))
                .build();
    }

    public ApiProblem conflict(ConflictException exception, WebRequest request) {
        return ApiProblem.builder()
                .status(HttpStatus.CONFLICT)
                .type(errorType("conflict"))
                .title(localize("error.conflict.title", "Conflict"))
                .detail(localize(
                        exception.getMessageKey(),
                        "The operation conflicts with the current state of the resource.",
                        exception.getArgs()
                ))
                .instance(resolveRequestInstance(request))
                .build();
    }

    public ApiProblem internalServerError(WebRequest request) {
        return ApiProblem.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .type(errorType("internal-server-error"))
                .title(localize("error.internal-server-error.title", "Internal Server Error"))
                .detail(localize(
                        "error.internal-server-error.detail",
                        "An unexpected error occurred."
                ))
                .instance(resolveRequestInstance(request))
                .build();
    }

    public ApiProblem validationError(
            MethodArgumentNotValidException exception, List<ApiError> errors, WebRequest request
    ) {
        ProblemDetail problemDetail = exception.updateAndGetBody(messageSource, request.getLocale());
        problemDetail.setType(errorType("validation-error"));

        if (problemDetail.getInstance() == null) {
            problemDetail.setInstance(resolveRequestInstance(request));
        }

        return new ApiProblem(problemDetail, errors);
    }


    private static URI errorType(String type) {
        return URI.create(BASE_ERROR_URI + "/" + type);
    }

    private static URI resolveRequestInstance(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return URI.create(servletRequest.getRequest().getRequestURI());
        }
        return null;
    }

    private String localize(String messageKey, String defaultMessage, Object... arguments) {
        return messageSource.getMessage(messageKey, arguments, defaultMessage, LocaleContextHolder.getLocale());
    }

}
