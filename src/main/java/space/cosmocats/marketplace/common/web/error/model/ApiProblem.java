package space.cosmocats.marketplace.common.web.error.model;

import lombok.*;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatusCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "title", "status", "detail", "instance", "errors"})
public class ApiProblem extends ProblemDetail {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<ApiError> errors;

    public ApiProblem(ProblemDetail problemDetail, List<ApiError> errors) {
        super(Objects.requireNonNull(problemDetail, "problemDetail must not be null"));
        this.errors = copyErrors(errors);
    }

    @Builder
    public ApiProblem(
            URI type, String title, HttpStatusCode status, String detail, URI instance, List<ApiError> errors
    ) {
        super(Objects.requireNonNull(status, "status must not be null").value());

        setType(type);
        setTitle(title);
        setDetail(detail);
        setInstance(instance);

        this.errors = copyErrors(errors);
    }

    private static List<ApiError> copyErrors(List<ApiError> errors) {
        return (errors == null) ? List.of() : List.copyOf(errors);
    }
}