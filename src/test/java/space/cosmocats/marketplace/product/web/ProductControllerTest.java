package space.cosmocats.marketplace.product.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Currency;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.stream.Stream;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;


import space.cosmocats.marketplace.common.config.LocaleConfig;
import space.cosmocats.marketplace.common.exception.ConflictException;
import space.cosmocats.marketplace.common.exception.DomainRuleViolationException;
import space.cosmocats.marketplace.common.pagination.PageResult;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.product.domain.exception.InsufficientStockException;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.domain.repository.ProductSort;
import space.cosmocats.marketplace.common.web.error.model.ApiProblemFactory;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.application.service.ProductService;
import space.cosmocats.marketplace.product.application.exception.ProductNotFoundException;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@WebMvcTest(ProductController.class)
@Import({ProductMapperImpl.class, ApiProblemFactory.class, LocaleConfig.class})
class ProductControllerTest {

    private static final String PRODUCTS_PATH = "/api/v1/products";
    private static final String ERROR_BASE_URI = "https://cosmo-cats.market/errors/";
    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_PRODUCT_ID = UUID.fromString("30000000-0000-4000-8000-000000000002");
    private static final UUID CATEGORY_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("Returns an existing product with all response fields")
    void shouldReturnProductById() throws Exception {
        // Given
        Product product = product();
        when(productService.getProductById(PRODUCT_ID)).thenReturn(product);

        // When
        MvcResult result = mockMvc.perform(get(PRODUCTS_PATH + "/{id}", PRODUCT_ID)).andReturn();

        // Then
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertProductJson(responseBody(result), product);
        verify(productService).getProductById(PRODUCT_ID);
    }

    @Test
    @DisplayName("Returns products using default pagination and sorting")
    void shouldReturnProductsWithDefaultCriteria() throws Exception {
        Product firstProduct = product();
        Product secondProduct = productWithOtherId();
        ProductCriteria criteria = new ProductCriteria(0, 20, ProductSort.PRICE_ASC);
        PageResult<Product> page = PageResult.<Product>builder()
                .content(List.of(firstProduct, secondProduct))
                .page(0).size(20).totalElements(2).totalPages(1)
                .build();
        when(productService.getAllProducts(criteria)).thenReturn(page);

        MvcResult result = mockMvc.perform(get(PRODUCTS_PATH)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = responseBody(result);
        assertThat(body.get("content").size()).isEqualTo(2);
        assertProductJson(body.get("content").get(0), firstProduct);
        assertProductJson(body.get("content").get(1), secondProduct);
        assertPageMetadata(body, 0, 20, 2, 1, true);
        verify(productService).getAllProducts(criteria);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ProductSort.class)
    @DisplayName("Passes explicit pagination and sorting to the service")
    void shouldPassExplicitCriteriaToService(ProductSort sort) throws Exception {
        Product product = product();
        ProductCriteria criteria = new ProductCriteria(1, 2, sort);
        PageResult<Product> page = PageResult.<Product>builder()
                .content(List.of(product, productWithOtherId()))
                .page(1).size(2).totalElements(5).totalPages(3)
                .build();
        when(productService.getAllProducts(criteria)).thenReturn(page);

        MvcResult result = mockMvc.perform(get(PRODUCTS_PATH)
                .param("page", "1").param("size", "2").param("sort", sort.name())).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = responseBody(result);
        assertThat(body.get("content").size()).isEqualTo(2);
        assertProductJson(body.get("content").get(0), product);
        assertPageMetadata(body, 1, 2, 5, 3, true);
        verify(productService).getAllProducts(criteria);
    }

    @Test
    @DisplayName("Returns an empty product page with status 200")
    void shouldReturnEmptyProductPage() throws Exception {
        ProductCriteria criteria = new ProductCriteria(0, 20, ProductSort.PRICE_ASC);
        PageResult<Product> page = PageResult.<Product>builder()
                .content(List.of()).page(0).size(20).totalElements(0).totalPages(0)
                .build();
        when(productService.getAllProducts(criteria)).thenReturn(page);

        MvcResult result = mockMvc.perform(get(PRODUCTS_PATH)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = responseBody(result);
        assertThat(body.get("content").isArray()).isTrue();
        assertThat(body.get("content").size()).isZero();
        assertPageMetadata(body, 0, 20, 0, 0, false);
        verify(productService).getAllProducts(criteria);
    }

    @Test
    @DisplayName("Creates a product and returns its representation and location")
    void shouldCreateProduct() throws Exception {
        Product product = product();
        String body = objectMapper.writeValueAsString(validRequestBody());
        when(productService.createProduct(any(Product.class))).thenReturn(product);

        MvcResult result = mockMvc.perform(writeRequest("POST").content(body)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(201);
        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo("http://localhost" + PRODUCTS_PATH + "/" + PRODUCT_ID);
        assertProductJson(responseBody(result), product);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productService).createProduct(captor.capture());
        assertThat(captor.getValue().getId()).isNotNull();
        assertThat(captor.getValue()).usingRecursiveComparison().ignoringFields("id").isEqualTo(product);
    }

    @Test
    @DisplayName("Updates a product using the path ID and request fields")
    void shouldUpdateProduct() throws Exception {
        Product product = product();
        String body = objectMapper.writeValueAsString(validRequestBody());
        when(productService.updateProduct(any(Product.class))).thenReturn(product);

        MvcResult result = mockMvc.perform(writeRequest("PUT").content(body)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertProductJson(responseBody(result), product);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productService).updateProduct(captor.capture());
        assertThat(captor.getValue()).usingRecursiveComparison().isEqualTo(product);
    }

    @Test
    @DisplayName("Deletes a product and returns status 204 without a body")
    void shouldDeleteProduct() throws Exception {
        UUID productId = PRODUCT_ID;

        MvcResult result = mockMvc.perform(delete(PRODUCTS_PATH + "/{id}", productId)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        assertThat(result.getResponse().getContentAsString()).isEmpty();
        verify(productService).deleteProduct(productId);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST", "PUT"})
    @DisplayName("Accepts zero stock when creating or updating a product")
    void shouldAcceptZeroStock(String method) throws Exception {
        Map<String, Object> body = validRequestBody();
        body.put("stock", 0);
        Product product = product().removeStock(new Quantity(5));
        if (method.equals("POST")) {
            when(productService.createProduct(any(Product.class))).thenReturn(product);
        } else {
            when(productService.updateProduct(any(Product.class))).thenReturn(product);
        }

        MvcResult result = mockMvc.perform(writeRequest(method)
                .content(objectMapper.writeValueAsString(body))).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(method.equals("POST") ? 201 : 200);
        assertProductJson(responseBody(result), product);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        if (method.equals("POST")) {
            verify(productService).createProduct(captor.capture());
        } else {
            verify(productService).updateProduct(captor.capture());
        }
        assertThat(captor.getValue().getStock().value()).isZero();
    }

    @ParameterizedTest(name = "[{index}] {0}: invalid {1}")
    @MethodSource("invalidProductRequests")
    @DisplayName("Rejects invalid product fields before calling the service")
    void shouldRejectInvalidProductFields(String method, String field, Object invalidValue) throws Exception {
        Map<String, Object> body = validRequestBody();
        body.put(field, invalidValue);

        MvcResult result = mockMvc.perform(writeRequest(method)
                .content(objectMapper.writeValueAsString(body))).andReturn();

        JsonNode problem = assertProblem(result, 400, ERROR_BASE_URI + "validation-error");
        assertThat(problem.get("title").asString()).isEqualTo("Validation error");
        assertThat(problem.get("detail").asString()).isNotBlank();
        assertThat(problem.get("errors")).isNotNull();
        assertThat(problem.get("errors").findValuesAsString("pointer")).contains("/" + field);
        for (JsonNode error : problem.get("errors")) {
            assertThat(error.get("detail").asString()).isNotBlank();
        }
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({"page, -1", "size, 0", "size, -1", "size, 101", "sort, UNKNOWN", "page, abc", "size, abc"})
    @DisplayName("Rejects invalid pagination and sorting parameters")
    void shouldRejectInvalidPageParameters(String parameter, String value) throws Exception {
        MockHttpServletRequestBuilder request = get(PRODUCTS_PATH).param(parameter, value);

        MvcResult result = mockMvc.perform(request).andReturn();

        JsonNode problem = assertProblem(result, 400, ERROR_BASE_URI + "validation-error");
        assertThat(problem.get("errors").findValuesAsString("pointer")).contains("/" + parameter);
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    @DisplayName("Rejects malformed product IDs in the request path")
    void shouldRejectMalformedProductId(String method) throws Exception {
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), PRODUCTS_PATH + "/not-a-uuid");
        if (method.equals("PUT")) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validRequestBody()));
        }

        MvcResult result = mockMvc.perform(request).andReturn();

        assertProblem(result, 400, "about:blank");
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"POST", "PUT"})
    @DisplayName("Rejects malformed category IDs in the JSON body")
    void shouldRejectMalformedCategoryId(String method) throws Exception {
        Map<String, Object> body = validRequestBody();
        body.put("categoryId", "not-a-uuid");

        MvcResult result = mockMvc.perform(writeRequest(method)
                .content(objectMapper.writeValueAsString(body))).andReturn();

        assertProblem(result, 400, "about:blank");
        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unreadableRequests")
    @DisplayName("Rejects malformed JSON and missing request bodies")
    void shouldRejectUnreadableRequestBody(String method, String body) throws Exception {
        MockHttpServletRequestBuilder request = writeRequest(method).content(body);

        MvcResult result = mockMvc.perform(request).andReturn();

        JsonNode problem = assertProblem(result, 400, "about:blank");
        assertThat(problem.get("title").asString()).isEqualTo("Invalid request body");
        assertThat(problem.get("detail").asString()).isEqualTo("Request body is missing or malformed.");
        verifyNoInteractions(productService);
    }


    @Test
    @DisplayName("Returns a localized 404 response when a product cannot be found")
    void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.getProductById(PRODUCT_ID)).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        MvcResult result = mockMvc.perform(get(PRODUCTS_PATH + "/{id}", PRODUCT_ID)).andReturn();

        JsonNode problem = assertProblem(result, 404, ERROR_BASE_URI + "not-found");
        assertThat(problem.get("title").asString()).isEqualTo("Not Found");
        assertThat(problem.get("detail").asString()).isEqualTo("Product was not found.");
        verify(productService).getProductById(PRODUCT_ID);
    }


    @Test
    @DisplayName("Returns status 404 when updating an unknown product")
    void shouldReturnNotFoundWhenUpdatingUnknownProduct() throws Exception {
        String body = objectMapper.writeValueAsString(validRequestBody());
        when(productService.updateProduct(any(Product.class))).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        MvcResult result = mockMvc.perform(put(PRODUCTS_PATH + "/{id}", PRODUCT_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn();

        JsonNode problem = assertProblem(result, 404, ERROR_BASE_URI + "not-found");
        assertThat(problem.get("detail").asString()).isEqualTo("Product was not found.");
        verify(productService).updateProduct(any(Product.class));
    }

    @Test
    @DisplayName("Returns status 404 when deleting an unknown product")
    void shouldReturnNotFoundWhenDeletingUnknownProduct() throws Exception {
        doThrow(new ProductNotFoundException(PRODUCT_ID)).when(productService).deleteProduct(PRODUCT_ID);

        MvcResult result = mockMvc.perform(delete(PRODUCTS_PATH + "/{id}", PRODUCT_ID)).andReturn();

        JsonNode problem = assertProblem(result, 404, ERROR_BASE_URI + "not-found");
        assertThat(problem.get("detail").asString()).isEqualTo("Product was not found.");
        verify(productService).deleteProduct(PRODUCT_ID);
    }

    @Test
    @DisplayName("Returns Ukrainian error messages when requested")
    void shouldReturnUkrainianErrorMessages() throws Exception {
        when(productService.getProductById(PRODUCT_ID)).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        MvcResult result = mockMvc.perform(get(PRODUCTS_PATH + "/{id}", PRODUCT_ID)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "uk")).andReturn();

        JsonNode problem = assertProblem(result, 404, ERROR_BASE_URI + "not-found");
        assertThat(problem.get("title").asString()).isEqualTo("Ресурс не знайдено");
        assertThat(problem.get("detail").asString()).isEqualTo("Товар не знайдено.");
        verify(productService).getProductById(PRODUCT_ID);
    }


    @Test
    @DisplayName("Returns status 422 with a formatted domain rule violation message")
    void shouldReturnUnprocessableContentForDomainRuleViolation() throws Exception {
        String body = objectMapper.writeValueAsString(validRequestBody());
        DomainRuleViolationException exception = new InsufficientStockException(PRODUCT_ID, "Star Lamp", 5, 6);
        when(productService.updateProduct(any(Product.class))).thenThrow(exception);

        MvcResult result = mockMvc.perform(writeRequest("PUT").content(body)).andReturn();

        JsonNode problem = assertProblem(result, 422, ERROR_BASE_URI + "domain-rule-violation");
        assertThat(problem.get("title").asString()).isEqualTo("Domain rule violation");
        assertThat(problem.get("detail").asString())
                .isEqualTo("Insufficient stock for product \"Star Lamp\": available 5, requested 6.");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(exception.getMessage());
        verify(productService).updateProduct(any(Product.class));
    }


    @Test
    @DisplayName("Returns status 409 with a safe fallback message for a state conflict")
    void shouldReturnConflictForStateConflict() throws Exception {
        String body = objectMapper.writeValueAsString(validRequestBody());
        ConflictException exception = new ConflictException(
                "error.test.product-conflict", "Product update conflicts with existing state"
        ) {};
        when(productService.updateProduct(any(Product.class))).thenThrow(exception);

        MvcResult result = mockMvc.perform(writeRequest("PUT").content(body)).andReturn();

        JsonNode problem = assertProblem(result, 409, ERROR_BASE_URI + "conflict");
        assertThat(problem.get("title").asString()).isEqualTo("Conflict");
        assertThat(problem.get("detail").asString())
                .isEqualTo("The operation conflicts with the current state of the resource.");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(exception.getMessage());
        verify(productService).updateProduct(any(Product.class));
    }

    @Test
    @DisplayName("Returns a safe 500 response for unexpected service exceptions")
    void shouldHideUnexpectedExceptionDetails() throws Exception {
        String internalMessage = "Sensitive service failure";
        when(productService.getProductById(PRODUCT_ID)).thenThrow(new IllegalStateException(internalMessage));

        MvcResult result = mockMvc.perform(get(PRODUCTS_PATH + "/{id}", PRODUCT_ID)).andReturn();

        JsonNode problem = assertProblem(result, 500, ERROR_BASE_URI + "internal-server-error");
        assertThat(problem.get("title").asString()).isEqualTo("Internal Server Error");
        assertThat(problem.get("detail").asString()).isEqualTo("An unexpected error occurred.");
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(internalMessage, "IllegalStateException", "stackTrace");
        verify(productService).getProductById(PRODUCT_ID);
    }

    private static Stream<Arguments> invalidProductRequests() {
        return Stream.of("POST", "PUT").flatMap(method -> Stream.of(
                new Object[]{"name", null},
                new Object[]{"name", ""},
                new Object[]{"name", "   "},
                new Object[]{"name", "st"},
                new Object[]{"name", "Star " + "x".repeat(96)},
                new Object[]{"name", "Orbit Lamp"},
                new Object[]{"name", "Starlight Lamp"},
                new Object[]{"description", null},
                new Object[]{"description", ""},
                new Object[]{"description", "   "},
                new Object[]{"description", "x".repeat(19)},
                new Object[]{"description", "x".repeat(501)},
                new Object[]{"price", null},
                new Object[]{"price", BigDecimal.ZERO},
                new Object[]{"price", new BigDecimal("-0.01")},
                new Object[]{"price", new BigDecimal("15.001")},
                new Object[]{"price", new BigDecimal("100000000000000000.00")},
                new Object[]{"currency", null},
                new Object[]{"currency", ""},
                new Object[]{"currency", "   "},
                new Object[]{"currency", "ZZZ"},
                new Object[]{"currency", "usd"},
                new Object[]{"stock", null},
                new Object[]{"stock", -1},
                new Object[]{"categoryId", null}
        ).map(invalidField -> Arguments.of(method, invalidField[0], invalidField[1])));
    }

    private static Stream<Arguments> unreadableRequests() {
        return Stream.of("POST", "PUT")
                .flatMap(method -> Stream.of("{", "").map(body -> Arguments.of(method, body)));
    }

    private MockHttpServletRequestBuilder writeRequest(String method) {
        String path = method.equals("POST") ? PRODUCTS_PATH : PRODUCTS_PATH + "/" + PRODUCT_ID;
        return request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON);
    }

    private Map<String, Object> validRequestBody() {
        Product product = product();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", product.getName());
        body.put("description", product.getDescription());
        body.put("price", product.getPrice().amount());
        body.put("currency", product.getPrice().currency().getCurrencyCode());
        body.put("stock", product.getStock().value());
        body.put("categoryId", product.getCategoryId());
        return body;
    }

    private Product product() {
        return Product.builder()
                .id(PRODUCT_ID)
                .name("Star Lamp")
                .description("A compact star lamp for controller tests.")
                .price(new Money(Currency.getInstance("USD"), new BigDecimal("15.50")))
                .stock(new Quantity(5))
                .categoryId(CATEGORY_ID)
                .build();
    }

    private Product productWithOtherId() {
        Product product = product();
        return Product.builder()
                .id(OTHER_PRODUCT_ID)
                .name("Galaxy Bowl")
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .categoryId(product.getCategoryId())
                .build();
    }

    private JsonNode responseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode assertProblem(MvcResult result, int expectedStatus, String expectedType) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        assertThat(result.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        JsonNode body = responseBody(result);
        assertThat(body.get("status").intValue()).isEqualTo(expectedStatus);
        assertThat(body.path("type").asString("about:blank")).isEqualTo(expectedType);
        assertThat(body.get("instance").asString()).isEqualTo(result.getRequest().getRequestURI());
        return body;
    }

    private void assertProductJson(JsonNode body, Product expected) {
        assertThat(body.get("id").asString()).isEqualTo(expected.getId().toString());
        assertThat(body.get("name").asString()).isEqualTo(expected.getName());
        assertThat(body.get("description").asString()).isEqualTo(expected.getDescription());
        assertThat(body.get("price").decimalValue()).isEqualByComparingTo(expected.getPrice().amount());
        assertThat(body.get("currency").asString()).isEqualTo(expected.getPrice().currency().getCurrencyCode());
        assertThat(body.get("stock").intValue()).isEqualTo(expected.getStock().value());
        assertThat(body.get("categoryId").asString()).isEqualTo(expected.getCategoryId().toString());
    }

    private void assertPageMetadata(JsonNode body, int page, int size, long totalElements, int totalPages, boolean hasContent) {
        assertThat(body.get("page").intValue()).isEqualTo(page);
        assertThat(body.get("size").intValue()).isEqualTo(size);
        assertThat(body.get("totalElements").longValue()).isEqualTo(totalElements);
        assertThat(body.get("totalPages").intValue()).isEqualTo(totalPages);
        assertThat(body.get("hasContent").booleanValue()).isEqualTo(hasContent);
    }
}
