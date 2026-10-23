package space.cosmocats.marketplace.product.web;

import java.util.List;
import java.util.UUID;
import java.util.Currency;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.stream.Stream;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.mockmvc.MockMvcRequest;
import com.atlassian.oai.validator.mockmvc.MockMvcResponse;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.LevelResolverFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;
import com.atlassian.oai.validator.report.ValidationReport;

import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import space.cosmocats.marketplace.common.config.LocaleConfig;
import space.cosmocats.marketplace.common.pagination.PageResult;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.product.web.dto.ProductRequest;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.domain.repository.ProductSort;
import space.cosmocats.marketplace.common.web.error.model.ApiProblemFactory;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.application.service.ProductService;
import space.cosmocats.marketplace.product.application.exception.ProductNotFoundException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@WebMvcTest(ProductController.class)
@Isolated("Shares the mocked ProductService with other MVC tests")
@Import({ProductMapperImpl.class, ApiProblemFactory.class, LocaleConfig.class})
class ProductContractTest {

    private static final String API_BASE_PATH = "/api/v1";
    private static final String ITEM_PATH = "/products/{id}";
    private static final String CONTEXT_PATH = "/marketplace";
    private static final String COLLECTION_PATH = "/products";
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");

    private static OpenApiInteractionValidator validator;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @BeforeAll
    static void loadContract() throws IOException {
        String specificationUrl = new ClassPathResource("static/api-specs/product-api.yml")
                .getURL()
                .toExternalForm();

        validator = OpenApiInteractionValidator.createForSpecificationUrl(specificationUrl)
                .withBasePathOverride(API_BASE_PATH)
                .withLevelResolver(LevelResolverFactory.withAdditionalPropertiesIgnored())
                .build();
    }

    @Test
    @DisplayName("Returns a product by ID according to the OpenAPI contract")
    void shouldReturnProductMatchingOpenApiContract() throws Exception {
        // Given
        Product product = product();
        when(productService.getProductById(PRODUCT_ID)).thenReturn(product);

        // When
        MvcResult result = mockMvc.perform(apiRequest("GET", ITEM_PATH)).andReturn();

        // Then
        assertValidInteraction(result, 200);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ProductSort.class)
    @DisplayName("Returns a product page according to the OpenAPI contract")
    void shouldReturnProductPageMatchingContract(ProductSort sort) throws Exception {
        ProductCriteria criteria = new ProductCriteria(0, 100, sort);
        PageResult<Product> page = PageResult.<Product>builder()
                .content(List.of(product()))
                .page(0).size(100).totalElements(1).totalPages(1)
                .build();
        when(productService.getAllProducts(criteria)).thenReturn(page);

        MvcResult result = mockMvc.perform(apiRequest("GET", COLLECTION_PATH)
                .param("page", "0").param("size", "100").param("sort", sort.name())).andReturn();

        assertValidInteraction(result, 200);
    }

    @Test
    @DisplayName("Returns an empty product page according to the OpenAPI contract")
    void shouldReturnEmptyPageMatchingContract() throws Exception {
        ProductCriteria criteria = new ProductCriteria(0, 20, ProductSort.PRICE_ASC);
        PageResult<Product> page = PageResult.<Product>builder()
                .content(List.of()).page(0).size(20).totalElements(0).totalPages(0)
                .build();
        when(productService.getAllProducts(criteria)).thenReturn(page);

        MvcResult result = mockMvc.perform(apiRequest("GET", COLLECTION_PATH)).andReturn();

        assertValidInteraction(result, 200);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST", "PUT"})
    @DisplayName("Accepts a product request without an ID and returns a contract-compliant product")
    void shouldWriteProductMatchingContract(String method) throws Exception {
        Product product = product();
        if (method.equals("POST")) {
            when(productService.createProduct(any(Product.class))).thenReturn(product);
        } else {
            when(productService.updateProduct(any(Product.class))).thenReturn(product);
        }
        String path = method.equals("POST") ? COLLECTION_PATH : ITEM_PATH;

        MvcResult result = mockMvc.perform(operationRequest(method, path)).andReturn();

        assertValidInteraction(result, method.equals("POST") ? 201 : 200);
        if (method.equals("POST")) {
            assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                    .isEqualTo("http://localhost" + CONTEXT_PATH + API_BASE_PATH + COLLECTION_PATH + "/" + PRODUCT_ID);
        }
    }

    @Test
    @DisplayName("Deletes a product according to the OpenAPI contract")
    void shouldDeleteProductMatchingContract() throws Exception {
        MockHttpServletRequestBuilder request = apiRequest("DELETE", ITEM_PATH);

        MvcResult result = mockMvc.perform(request).andReturn();

        assertValidInteraction(result, 204);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({"page, -1", "size, 101", "sort, UNKNOWN", "page, abc"})
    @DisplayName("Returns a contract-compliant validation error for invalid page parameters")
    void shouldReturnPageValidationErrorMatchingContract(String parameter, String value) throws Exception {
        MockHttpServletRequestBuilder request = apiRequest("GET", COLLECTION_PATH).param(parameter, value);

        MvcResult result = mockMvc.perform(request).andReturn();

        assertValidResponse(result, 400);
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST", "PUT"})
    @DisplayName("Returns a contract-compliant validation error for invalid product fields")
    void shouldReturnProductValidationErrorMatchingContract(String method) throws Exception {
        ObjectNode body = objectMapper.valueToTree(validProductRequest());
        body.put("price", 0);
        String path = method.equals("POST") ? COLLECTION_PATH : ITEM_PATH;

        MvcResult result = mockMvc.perform(apiRequest(method, path)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))).andReturn();

        assertValidResponse(result, 400);
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unreadableRequests")
    @DisplayName("Returns a contract-compliant error for malformed or missing request bodies")
    void shouldReturnUnreadableBodyErrorMatchingContract(String method, String body) throws Exception {
        String path = method.equals("POST") ? COLLECTION_PATH : ITEM_PATH;
        MockHttpServletRequestBuilder request = apiRequest(method, path)
                .contentType(MediaType.APPLICATION_JSON).content(body);

        MvcResult result = mockMvc.perform(request).andReturn();

        JsonNode problem = assertValidResponse(result, 400);
        assertThat(problem.path("type").asString("about:blank")).isEqualTo("about:blank");
        assertThat(problem.get("title").asString()).isEqualTo("Invalid request body");
        assertThat(problem.get("detail").asString()).isEqualTo("Request body is missing or malformed.");
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST", "PUT"})
    @DisplayName("Returns a contract-compliant error for malformed category IDs")
    void shouldReturnCategoryIdErrorMatchingContract(String method) throws Exception {
        ObjectNode body = objectMapper.valueToTree(validProductRequest());
        body.put("categoryId", "not-a-uuid");
        String path = method.equals("POST") ? COLLECTION_PATH : ITEM_PATH;

        MvcResult result = mockMvc.perform(apiRequest(method, path)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))).andReturn();

        JsonNode problem = assertValidResponse(result, 400);
        assertThat(problem.path("type").asString("about:blank")).isEqualTo("about:blank");
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    @DisplayName("Returns a contract-compliant error for malformed product IDs")
    void shouldReturnProductIdErrorMatchingContract(String method) throws Exception {
        MockHttpServletRequestBuilder request = operationRequest(method, "/products/not-a-uuid");

        MvcResult result = mockMvc.perform(request).andReturn();

        JsonNode problem = assertValidResponse(result, 400);
        assertThat(problem.path("type").asString("about:blank")).isEqualTo("about:blank");
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    @DisplayName("Returns a contract-compliant error when a product is not found")
    void shouldReturnNotFoundMatchingContract(String method) throws Exception {
        stubServiceFailure(method, ITEM_PATH, new ProductNotFoundException(PRODUCT_ID));

        MvcResult result = mockMvc.perform(operationRequest(method, ITEM_PATH)).andReturn();

        assertValidInteraction(result, 404);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST", "PUT"})
    @DisplayName("Returns a contract-compliant error for unsupported media types")
    void shouldReturnUnsupportedMediaTypeMatchingContract(String method) throws Exception {
        String path = method.equals("POST") ? COLLECTION_PATH : ITEM_PATH;
        MockHttpServletRequestBuilder request = operationRequest(method, path).contentType(MediaType.TEXT_PLAIN);

        MvcResult result = mockMvc.perform(request).andReturn();

        assertValidResponse(result, 415);
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("productOperations")
    @DisplayName("Returns a contract-compliant server error for every product operation")
    void shouldReturnServerErrorMatchingContract(String method, String path) throws Exception {
        stubServiceFailure(method, path, new IllegalStateException("Sensitive service failure"));

        MvcResult result = mockMvc.perform(operationRequest(method, path)).andReturn();

        assertValidInteraction(result, 500);
    }

    @Test
    @DisplayName("Detects a product response that violates the OpenAPI schema")
    void shouldDetectResponseViolatingContract() throws Exception {
        Product product = product().withDescription("short");
        when(productService.getProductById(PRODUCT_ID)).thenReturn(product);

        MvcResult result = mockMvc.perform(apiRequest("GET", ITEM_PATH)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Request request = MockMvcRequest.of(result.getRequest());
        ValidationReport report = validator.validateResponse(request.getPath(), request.getMethod(),
                MockMvcResponse.of(result.getResponse()));
        assertThat(report.hasErrors()).isTrue();
        assertThat(report.getMessages()).extracting(ValidationReport.Message::getKey)
                .contains("validation.response.body.schema.minLength");
    }

    private static Stream<Arguments> unreadableRequests() {
        return Stream.of("POST", "PUT").flatMap(method ->
                Stream.of("{", "").map(body -> Arguments.of(method, body)));
    }

    private static Stream<Arguments> productOperations() {
        return Stream.of(
                Arguments.of("GET", COLLECTION_PATH),
                Arguments.of("POST", COLLECTION_PATH),
                Arguments.of("GET", ITEM_PATH),
                Arguments.of("PUT", ITEM_PATH),
                Arguments.of("DELETE", ITEM_PATH)
        );
    }

    private MockHttpServletRequestBuilder apiRequest(String method, String path) {
        String expandedPath = path.replace("{id}", PRODUCT_ID.toString());
        return request(HttpMethod.valueOf(method), CONTEXT_PATH + API_BASE_PATH + expandedPath)
                .contextPath(CONTEXT_PATH);
    }

    private MockHttpServletRequestBuilder operationRequest(String method, String path) throws Exception {
        MockHttpServletRequestBuilder request = apiRequest(method, path);
        if (method.equals("POST") || method.equals("PUT")) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validProductRequest()));
        }
        return request;
    }

    private void stubServiceFailure(String method, String path, RuntimeException exception) {
        switch (method) {
            case "GET" -> {
                if (path.equals(COLLECTION_PATH)) {
                    when(productService.getAllProducts(any(ProductCriteria.class))).thenThrow(exception);
                } else {
                    when(productService.getProductById(PRODUCT_ID)).thenThrow(exception);
                }
            }
            case "POST" -> when(productService.createProduct(any(Product.class))).thenThrow(exception);
            case "PUT" -> when(productService.updateProduct(any(Product.class))).thenThrow(exception);
            case "DELETE" -> doThrow(exception).when(productService).deleteProduct(PRODUCT_ID);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    private ProductRequest validProductRequest() {
        Product product = product();
        return new ProductRequest(product.getName(), product.getDescription(), product.getPrice().amount(),
                product.getPrice().currency().getCurrencyCode(), product.getStock().value(), product.getCategoryId());
    }

    private Product product() {
        return Product.builder()
                .id(PRODUCT_ID)
                .name("Star Lamp")
                .description("A compact star lamp for contract tests.")
                .price(new Money(Currency.getInstance("USD"), new BigDecimal("15.50")))
                .stock(new Quantity(5))
                .categoryId(CATEGORY_ID)
                .build();
    }

    private void assertValidInteraction(MvcResult result, int expectedStatus) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        openApi().isValid(validator).match(result);
    }

    private JsonNode assertValidResponse(MvcResult result, int expectedStatus) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);

        Request request = MockMvcRequest.of(result.getRequest());
        ValidationReport report = validator.validateResponse(request.getPath(), request.getMethod(),
                MockMvcResponse.of(result.getResponse()));
        assertThat(report.hasErrors())
                .as("OpenAPI response validation errors: %s", report.getMessages()).isFalse();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("status").intValue()).isEqualTo(expectedStatus);
        assertThat(body.get("instance").asString()).isEqualTo(result.getRequest().getRequestURI());

        return body;
    }
}
