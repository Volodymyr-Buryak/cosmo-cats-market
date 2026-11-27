package space.cosmocats.marketplace.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiSpecificationTest {

    private static final List<String> SPECIFICATIONS = List.of(
            "product-api.yml", "category-api.yml", "order-api.yml", "report-api.yml"
    );
    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "put", "post", "delete", "options", "head", "patch", "trace"
    );
    private static final YAMLMapper YAML_MAPPER = new YAMLMapper();

    @ParameterizedTest
    @ValueSource(strings = {"product-api.yml", "category-api.yml", "order-api.yml", "report-api.yml"})
    @DisplayName("should parse every published OpenAPI specification without errors")
    void shouldParseEverySpecificationWithoutErrors(String specification) throws IOException {
        var result = new OpenAPIV3Parser().readContents(readSpecification(specification), null, new ParseOptions());

        assertThat(result.getOpenAPI()).as("Parsed model for %s", specification).isNotNull();
        assertThat(result.getMessages()).as("Parser messages for %s", specification).isEmpty();
        assertThat(result.getOpenAPI().getOpenapi()).as("OpenAPI version for %s", specification).isEqualTo("3.1.0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"product-api.yml", "category-api.yml", "order-api.yml", "report-api.yml"})
    @DisplayName("should resolve every local reference within its specification")
    void shouldResolveEveryLocalReference(String specification) throws IOException {
        JsonNode document = YAML_MAPPER.readTree(readSpecification(specification));

        assertReferencesResolve(document, document, specification);
    }

    @Test
    @DisplayName("should use unique operation IDs across all published specifications")
    void shouldUseUniqueOperationIdsAcrossSpecifications() throws IOException {
        Map<String, String> operationLocations = new HashMap<>();

        for (String specification : SPECIFICATIONS) {
            JsonNode document = YAML_MAPPER.readTree(readSpecification(specification));
            JsonNode paths = document.path("paths");
            assertThat(paths.isObject()).as("Paths in %s", specification).isTrue();

            var pathEntries = paths.fields();
            while (pathEntries.hasNext()) {
                var pathEntry = pathEntries.next();
                for (String method : HTTP_METHODS) {
                    JsonNode operation = pathEntry.getValue().get(method);
                    if (operation == null) {
                        continue;
                    }

                    String location = specification + " " + method + " " + pathEntry.getKey();
                    JsonNode operationId = operation.path("operationId");
                    assertThat(operationId.isTextual()).as("Operation ID at %s", location).isTrue();
                    assertThat(operationId.asText()).as("Operation ID at %s", location).isNotBlank();
                    String previousLocation = operationLocations.putIfAbsent(operationId.asText(), location);
                    assertThat(previousLocation)
                            .as("Duplicate operation ID %s at %s; first used at %s",
                                    operationId.asText(), location, previousLocation)
                            .isNull();
                }
            }
        }

        assertThat(operationLocations).as("Published operations").isNotEmpty();
    }

    private static void assertReferencesResolve(JsonNode node, JsonNode document, String specification) {
        if (node.isObject() && node.has("$ref")) {
            JsonNode reference = node.get("$ref");
            assertThat(reference.isTextual()).as("Reference in %s", specification).isTrue();
            String pointer = reference.asText();
            assertThat(pointer).as("Standalone reference in %s", specification).startsWith("#/");
            assertThat(document.at(pointer.substring(1)).isMissingNode())
                    .as("Unresolved reference %s in %s", pointer, specification)
                    .isFalse();
        }

        for (JsonNode child : node) {
            assertReferencesResolve(child, document, specification);
        }
    }

    private static String readSpecification(String specification) throws IOException {
        return new ClassPathResource("static/api-specs/" + specification).getContentAsString(StandardCharsets.UTF_8);
    }
}
