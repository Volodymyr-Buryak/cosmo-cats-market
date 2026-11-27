package space.cosmocats.marketplace.web;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.MockMvcRequest;
import com.atlassian.oai.validator.mockmvc.MockMvcResponse;
import com.atlassian.oai.validator.report.LevelResolverFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import space.cosmocats.marketplace.common.config.LocaleConfig;
import space.cosmocats.marketplace.common.web.error.model.ApiProblemFactory;
import space.cosmocats.marketplace.order.application.command.CreateOrderCommand;
import space.cosmocats.marketplace.order.application.exception.OrderConflictException;
import space.cosmocats.marketplace.order.application.exception.OrderNotFoundException;
import space.cosmocats.marketplace.order.application.service.OrderReportService;
import space.cosmocats.marketplace.order.application.service.OrderService;
import space.cosmocats.marketplace.order.domain.exception.InvalidOrderStateTransitionException;
import space.cosmocats.marketplace.order.domain.model.Order;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.order.domain.model.PopularProduct;
import space.cosmocats.marketplace.order.domain.model.value.OrderItem;
import space.cosmocats.marketplace.order.web.OrderController;
import space.cosmocats.marketplace.order.web.OrderMapperImpl;
import space.cosmocats.marketplace.order.web.OrderReportController;
import space.cosmocats.marketplace.order.web.OrderReportMapperImpl;
import space.cosmocats.marketplace.product.application.exception.CategoryConflictException;
import space.cosmocats.marketplace.product.application.exception.CategoryNotFoundException;
import space.cosmocats.marketplace.product.application.service.CategoryService;
import space.cosmocats.marketplace.product.domain.exception.CategoryHierarchyCycleException;
import space.cosmocats.marketplace.product.domain.model.Category;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.web.controller.CategoryController;
import space.cosmocats.marketplace.product.web.mapper.CategoryMapperImpl;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@WebMvcTest({CategoryController.class, OrderController.class, OrderReportController.class})
@Import({CategoryMapperImpl.class, OrderMapperImpl.class, OrderReportMapperImpl.class,
        ApiProblemFactory.class, LocaleConfig.class})
@Isolated("Contract tests share mocked services with MVC tests")
class MarketplaceContractTest {
    private static final UUID ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final String CONTEXT_PATH = "/marketplace";
    private static final String BASE_PATH = "/api/v1";
    private static final String CATEGORY_BODY = "{\"name\":\"Equipment\",\"description\":\"Space equipment\",\"parentId\":null}";
    private static final String ORDER_BODY = "{\"buyerId\":\"%s\",\"items\":[{\"productId\":\"%s\",\"quantity\":2}]}".formatted(ID, ID);
    private static Map<String, OpenApiInteractionValidator> validators;

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private OrderService orderService;
    @MockitoBean
    private OrderReportService reportService;

    @BeforeAll
    static void loadContracts() throws IOException {
        validators = Map.of("category", loadContract("category"), "order", loadContract("order"),
                "report", loadContract("report"));
    }

    private static OpenApiInteractionValidator loadContract(String name) throws IOException {
        String url = new ClassPathResource("static/api-specs/" + name + "-api.yml").getURL().toExternalForm();
        return OpenApiInteractionValidator.createForSpecificationUrl(url)
                .withBasePathOverride(BASE_PATH)
                .withLevelResolver(LevelResolverFactory.withAdditionalPropertiesIgnored()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "read", "lookup", "create", "update", "delete"})
    @DisplayName("Category operations follow the published contract")
    void shouldMatchCategoryContract(String operation) throws Exception {
        MockHttpServletRequestBuilder request;
        int expectedStatus = 200;
        switch (operation) {
            case "list" -> {
                when(categoryService.getAllCategories()).thenReturn(List.of(category()));
                request = apiRequest("GET", "/categories");
            }
            case "read" -> {
                when(categoryService.getCategoryById(ID)).thenReturn(category());
                request = apiRequest("GET", "/categories/" + ID);
            }
            case "lookup" -> {
                when(categoryService.findCategoryByName("Equipment")).thenReturn(Optional.of(category()));
                request = apiRequest("GET", "/categories/by-name").param("name", "Equipment");
            }
            case "create" -> {
                when(categoryService.createCategory(any(Category.class))).thenReturn(category());
                request = apiRequest("POST", "/categories").contentType(MediaType.APPLICATION_JSON).content(CATEGORY_BODY);
                expectedStatus = 201;
            }
            case "update" -> {
                when(categoryService.updateCategory(any(Category.class))).thenReturn(category());
                request = apiRequest("PUT", "/categories/" + ID).contentType(MediaType.APPLICATION_JSON).content(CATEGORY_BODY);
            }
            case "delete" -> {
                request = apiRequest("DELETE", "/categories/" + ID);
                expectedStatus = 204;
            }
            default -> throw new IllegalArgumentException(operation);
        }

        MvcResult result = mockMvc.perform(request).andReturn();

        assertInteraction("category", result, expectedStatus);
        if (expectedStatus == 201) {
            assertThat(result.getResponse().getHeader("Location"))
                    .isEqualTo("http://localhost/marketplace/api/v1/categories/" + ID);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"list", "read", "create", "status", "delete"})
    @DisplayName("Order operations follow the published contract")
    void shouldMatchOrderContract(String operation) throws Exception {
        MockHttpServletRequestBuilder request;
        int expectedStatus = 200;
        switch (operation) {
            case "list" -> {
                when(orderService.getAllOrders()).thenReturn(List.of(order(OrderStatus.CREATED)));
                request = apiRequest("GET", "/orders");
            }
            case "read" -> {
                when(orderService.getOrderById(ID)).thenReturn(order(OrderStatus.CREATED));
                request = apiRequest("GET", "/orders/" + ID);
            }
            case "create" -> {
                when(orderService.createOrder(any(CreateOrderCommand.class))).thenReturn(order(OrderStatus.CREATED));
                request = apiRequest("POST", "/orders").contentType(MediaType.APPLICATION_JSON).content(ORDER_BODY);
                expectedStatus = 201;
            }
            case "status" -> {
                when(orderService.updateStatus(ID, OrderStatus.PAID)).thenReturn(order(OrderStatus.PAID));
                request = apiRequest("PATCH", "/orders/" + ID + "/status")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}");
            }
            case "delete" -> {
                request = apiRequest("DELETE", "/orders/" + ID);
                expectedStatus = 204;
            }
            default -> throw new IllegalArgumentException(operation);
        }

        MvcResult result = mockMvc.perform(request).andReturn();

        assertInteraction("order", result, expectedStatus);
        if (expectedStatus == 201) {
            assertThat(result.getResponse().getHeader("Location"))
                    .isEqualTo("http://localhost/marketplace/api/v1/orders/" + ID);
        }
    }

    @ParameterizedTest
    @EnumSource(OrderStatus.class)
    @DisplayName("All persisted order statuses are valid contract responses")
    void shouldDescribeEveryOrderStatus(OrderStatus status) throws Exception {
        when(orderService.getOrderById(ID)).thenReturn(order(status));

        MvcResult result = mockMvc.perform(apiRequest("GET", "/orders/" + ID)).andReturn();

        assertInteraction("order", result, 200);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Popularity reports allow populated and empty results")
    void shouldMatchReportContract(boolean empty) throws Exception {
        List<PopularProduct> products = (empty) ? List.of() : List.of(new PopularProduct(ID, "Star Lamp", 9));
        when(reportService.getPopularProducts(10)).thenReturn(products);

        MvcResult result = mockMvc.perform(apiRequest("GET", "/reports/popular-products")).andReturn();

        assertInteraction("report", result, 200);
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    @DisplayName("Invalid requests produce contract-compliant error responses")
    void shouldDescribeInvalidRequests(String spec, String method, String path, String body, int status) throws Exception {
        var request = apiRequest(method, path).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(body);
        }

        MvcResult result = mockMvc.perform(request).andReturn();

        assertResponse(spec, result, status);
        verifyNoInteractions(categoryService, orderService, reportService);
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("category", "POST", "/categories", "{}", 400),
                Arguments.of("category", "PUT", "/categories/" + ID, "{}", 400),
                Arguments.of("category", "POST", "/categories", "{", 400),
                Arguments.of("category", "POST", "/categories", null, 400),
                Arguments.of("category", "GET", "/categories/not-a-uuid", null, 400),
                Arguments.of("category", "GET", "/categories/by-name", null, 400),
                Arguments.of("order", "POST", "/orders", "{}", 400),
                Arguments.of("order", "POST", "/orders", "{", 400),
                Arguments.of("order", "POST", "/orders", null, 400),
                Arguments.of("order", "PATCH", "/orders/" + ID + "/status", "{}", 400),
                Arguments.of("order", "PATCH", "/orders/" + ID + "/status", "{\"status\":\"UNKNOWN\"}", 400),
                Arguments.of("order", "GET", "/orders/not-a-uuid", null, 400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "101", "invalid"})
    @DisplayName("Report query errors follow the error contract")
    void shouldDescribeInvalidReportLimit(String limit) throws Exception {
        MvcResult result = mockMvc.perform(apiRequest("GET", "/reports/popular-products").param("limit", limit)).andReturn();

        assertResponse("report", result, 400);
        verifyNoInteractions(reportService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"category", "order"})
    @DisplayName("Unsupported body media types follow the error contract")
    void shouldDescribeUnsupportedMediaType(String spec) throws Exception {
        String path = (spec.equals("category")) ? "/categories" : "/orders";

        MvcResult result = mockMvc.perform(apiRequest("POST", path).contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andReturn();

        assertResponse(spec, result, 415);
        verifyNoInteractions(categoryService, orderService);
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 409, 422, 500})
    @DisplayName("Category service errors follow the error contract")
    void shouldDescribeCategoryServiceErrors(int status) throws Exception {
        RuntimeException exception = switch (status) {
            case 404 -> new CategoryNotFoundException(ID);
            case 409 -> new CategoryConflictException(ID);
            case 422 -> new CategoryHierarchyCycleException(ID);
            default -> new IllegalStateException("Internal diagnostic");
        };
        when(categoryService.updateCategory(any(Category.class))).thenThrow(exception);

        MvcResult result = mockMvc.perform(apiRequest("PUT", "/categories/" + ID)
                .contentType(MediaType.APPLICATION_JSON).content(CATEGORY_BODY)).andReturn();

        assertResponse("category", result, status);
    }

    @Test
    @DisplayName("Unknown category names follow the lookup error contract")
    void shouldDescribeMissingNaturalId() throws Exception {
        MvcResult result = mockMvc.perform(apiRequest("GET", "/categories/by-name").param("name", "Unknown")).andReturn();

        assertResponse("category", result, 404);
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 409, 422, 500})
    @DisplayName("Order status service errors follow the error contract")
    void shouldDescribeOrderStatusErrors(int status) throws Exception {
        RuntimeException exception = switch (status) {
            case 404 -> new OrderNotFoundException(ID);
            case 409 -> new OrderConflictException(ID);
            case 422 -> new InvalidOrderStateTransitionException(ID, OrderStatus.CREATED, OrderStatus.SHIPPED);
            default -> new IllegalStateException("Internal diagnostic");
        };
        when(orderService.updateStatus(ID, OrderStatus.SHIPPED)).thenThrow(exception);

        MvcResult result = mockMvc.perform(apiRequest("PATCH", "/orders/" + ID + "/status")
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}")).andReturn();

        assertResponse("order", result, status);
    }

    @Test
    @DisplayName("Unexpected report failures follow the safe server error contract")
    void shouldDescribeReportFailure() throws Exception {
        when(reportService.getPopularProducts(10)).thenThrow(new IllegalStateException("Internal diagnostic"));

        MvcResult result = mockMvc.perform(apiRequest("GET", "/reports/popular-products")).andReturn();

        assertResponse("report", result, 500);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("Internal diagnostic");
    }

    private MockHttpServletRequestBuilder apiRequest(String method, String path) {
        return request(HttpMethod.valueOf(method), CONTEXT_PATH + BASE_PATH + path).contextPath(CONTEXT_PATH);
    }

    private void assertInteraction(String spec, MvcResult result, int expectedStatus) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        openApi().isValid(validators.get(spec)).match(result);
    }

    private void assertResponse(String spec, MvcResult result, int expectedStatus) {
        assertThat(result.getResponse().getStatus()).isEqualTo(expectedStatus);
        var request = MockMvcRequest.of(result.getRequest());
        var report = validators.get(spec).validateResponse(request.getPath(), request.getMethod(),
                MockMvcResponse.of(result.getResponse()));
        assertThat(report.hasErrors()).as("Contract violations: %s", report.getMessages()).isFalse();
    }

    private Category category() {
        return Category.builder().id(ID).name("Equipment").description("Space equipment").build();
    }

    private Order order(OrderStatus status) {
        return Order.restoreBuilder().id(ID).buyerId(ID).createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .status(status).items(List.of(OrderItem.builder().productId(ID).productName("Star Lamp")
                        .unitPrice(new Money(Currency.getInstance("USD"), new BigDecimal("10.123")))
                        .quantity(new Quantity(2)).build())).build();
    }
}
