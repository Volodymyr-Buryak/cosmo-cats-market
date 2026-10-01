package space.cosmocats.marketplace.api.http.error.dto;

import lombok.*;
import java.util.List;
import org.springframework.http.ProblemDetail;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "title", "status", "detail", "instance", "errors"})
public class ApiProblem extends ProblemDetail {
    List<ApiError> errors;

    public ApiProblem(ProblemDetail problemDetail, List<ApiError> apiErrors) {
        super(problemDetail);
        this.errors = apiErrors;
    }
}
