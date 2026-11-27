package space.cosmocats.marketplace.web;

import java.util.List;
import java.util.Optional;
import space.cosmocats.marketplace.order.web.OrderReportController;
import space.cosmocats.marketplace.order.web.OrderReportMapperImpl;
import space.cosmocats.marketplace.order.application.service.OrderReportService;
import space.cosmocats.marketplace.order.domain.model.PopularProduct;
import java.util.UUID;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.MediaType;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import space.cosmocats.marketplace.common.config.LocaleConfig;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import space.cosmocats.marketplace.common.web.error.model.ApiProblemFactory;

import space.cosmocats.marketplace.product.domain.model.Category;
import space.cosmocats.marketplace.product.application.service.CategoryService;
import space.cosmocats.marketplace.product.application.exception.CategoryNotFoundException;

import space.cosmocats.marketplace.order.domain.model.Order;
import space.cosmocats.marketplace.order.web.OrderController;
import space.cosmocats.marketplace.order.web.OrderMapperImpl;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.order.domain.model.value.OrderItem;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.order.application.service.OrderService;
import space.cosmocats.marketplace.product.web.mapper.CategoryMapperImpl;
import space.cosmocats.marketplace.product.web.controller.CategoryController;
import space.cosmocats.marketplace.order.application.command.CreateOrderCommand;


import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@WebMvcTest({CategoryController.class, OrderController.class, OrderReportController.class})
@Import({CategoryMapperImpl.class, OrderMapperImpl.class, OrderReportMapperImpl.class, ApiProblemFactory.class, LocaleConfig.class})
class MarketplaceControllersTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String CATEGORY_JSON = "{\"name\":\"Equipment\",\"description\":\"Space equipment\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderReportService reportService;

    @Test
    @DisplayName("Creates a category and returns its location")
    void shouldCreateCategory() throws Exception {
        when(categoryService.createCategory(any(Category.class))).thenReturn(category());
        mockMvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content(CATEGORY_JSON))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/v1/categories/" + ID))
                .andExpect(jsonPath("$.name").value("Equipment"));
    }

    @Test
    @DisplayName("Lists and reads categories")
    void shouldReadCategories() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(category()));
        when(categoryService.getCategoryById(ID)).thenReturn(category());
        mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(ID.toString()));
        mockMvc.perform(get("/api/v1/categories/{id}", ID)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    @DisplayName("Updates a category using the path ID")
    void shouldUpdateCategory() throws Exception {
        when(categoryService.updateCategory(any(Category.class))).thenReturn(category());
        mockMvc.perform(put("/api/v1/categories/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(CATEGORY_JSON))
                .andExpect(status().isOk());
        verify(categoryService).updateCategory(category());
    }

    @Test
    @DisplayName("Deletes a category without a response body")
    void shouldDeleteCategory() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/{id}", ID)).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(categoryService).deleteCategory(ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\" \",\"description\":\"valid\"}", "{\"name\":\"valid\",\"description\":\" \"}"})
    @DisplayName("Rejects invalid categories before calling the service")
    void shouldValidateCategory(String body) throws Exception {
        mockMvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/v1/categories/{id}", ID).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(categoryService);
    }

    @Test
    @DisplayName("Returns the shared error response for missing categories")
    void shouldReportMissingCategory() throws Exception {
        when(categoryService.getCategoryById(ID)).thenThrow(new CategoryNotFoundException(ID));
        mockMvc.perform(get("/api/v1/categories/{id}", ID)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Creates an order from product references")
    void shouldCreateOrder() throws Exception {
        when(orderService.createOrder(any(CreateOrderCommand.class))).thenReturn(order());
        String body = "{\"buyerId\":\"%s\",\"items\":[{\"productId\":\"%s\",\"quantity\":2}]}".formatted(ID, ID);
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().string("Location", "http://localhost/api/v1/orders/" + ID))
                .andExpect(jsonPath("$.total").value(20)).andExpect(jsonPath("$.items[0].quantity").value(2));
        verify(orderService).createOrder(new CreateOrderCommand(ID, List.of(new CreateOrderCommand.Item(ID, 2))));
    }

    @Test
    @DisplayName("Lists and reads orders")
    void shouldReadOrders() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(order()));
        when(orderService.getOrderById(ID)).thenReturn(order());
        mockMvc.perform(get("/api/v1/orders")).andExpect(status().isOk()).andExpect(jsonPath("$[0].currency").value("USD"));
        mockMvc.perform(get("/api/v1/orders/{id}", ID)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("Changes an order status")
    void shouldUpdateOrderStatus() throws Exception {
        Order paid = order();
        paid.markAsPaid();
        when(orderService.updateStatus(ID, OrderStatus.PAID)).thenReturn(paid);
        mockMvc.perform(patch("/api/v1/orders/{id}/status", ID).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Deletes an order")
    void shouldDeleteOrder() throws Exception {
        mockMvc.perform(delete("/api/v1/orders/{id}", ID)).andExpect(status().isNoContent());
        verify(orderService).deleteOrder(ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"buyerId\":null,\"items\":[]}", "{\"buyerId\":\"00000000-0000-0000-0000-000000000001\",\"items\":[null]}", "{\"buyerId\":\"00000000-0000-0000-0000-000000000001\",\"items\":[{\"productId\":null,\"quantity\":0}]}"})
    @DisplayName("Validates orders including nested items")
    void shouldValidateOrder(String body) throws Exception {
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}"})
    @DisplayName("Rejects missing and unknown statuses")
    void shouldValidateStatus(String body) throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{id}/status", ID).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Rejects malformed resource IDs")
    void shouldRejectMalformedIds() throws Exception {
        mockMvc.perform(get("/api/v1/categories/not-a-uuid")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/orders/not-a-uuid")).andExpect(status().isBadRequest());
        verifyNoInteractions(categoryService, orderService);
    }

    @Test
    @DisplayName("Rejects unsupported request content types")
    void shouldRejectUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/api/v1/categories").contentType(MediaType.TEXT_PLAIN).content(CATEGORY_JSON))
                .andExpect(status().isUnsupportedMediaType());
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(categoryService, orderService);
    }

    @Test
    @DisplayName("Returns structured validation errors for nested order items")
    void shouldIncludeNestedFieldPointerForInvalidOrderItem() throws Exception {
        String body = "{\"buyerId\":\"%s\",\"items\":[{\"productId\":\"%s\",\"quantity\":1},{\"productId\":\"%s\",\"quantity\":0}]}"
                .formatted(ID, ID, ID);
        mockMvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/validation-error"))
                .andExpect(jsonPath("$.instance").value("/api/v1/orders"))
                .andExpect(jsonPath("$.errors[0].pointer").value("/items/1/quantity"));
        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("Finds a category by its natural ID")
    void shouldFindCategoryByName() throws Exception {
        when(categoryService.findCategoryByName("Equipment")).thenReturn(Optional.of(category()));

        mockMvc.perform(get("/api/v1/categories/by-name").param("name", "Equipment"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    @DisplayName("Reports an unknown natural ID using the common error format")
    void shouldReportUnknownCategoryName() throws Exception {
        mockMvc.perform(get("/api/v1/categories/by-name").param("name", "Missing"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "101", "invalid"})
    @DisplayName("Rejects invalid popularity report limits")
    void shouldValidateReportLimit(String limit) throws Exception {
        mockMvc.perform(get("/api/v1/reports/popular-products").param("limit", limit))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(reportService);
    }

    @Test
    @DisplayName("Returns the default popularity report")
    void shouldReturnPopularityReport() throws Exception {
        when(reportService.getPopularProducts(10)).thenReturn(List.of(new PopularProduct(ID, "Star Lamp", 9)));

        mockMvc.perform(get("/api/v1/reports/popular-products"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].productId").value(ID.toString()))
                .andExpect(jsonPath("$[0].productName").value("Star Lamp"))
                .andExpect(jsonPath("$[0].quantitySold").value(9));
    }

    @Test
    @DisplayName("Passes an explicit popularity limit to the service")
    void shouldPassExplicitReportLimit() throws Exception {
        when(reportService.getPopularProducts(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/reports/popular-products").param("limit", "1"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(reportService).getPopularProducts(1);
    }

    private Category category() {
        return Category.builder().id(ID).name("Equipment").description("Space equipment").build();
    }

    private Order order() {
        return Order.builder().id(ID).buyerId(ID).createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .items(List.of(OrderItem.builder().productId(ID).productName("Equipment")
                        .unitPrice(new Money(Currency.getInstance("USD"), BigDecimal.TEN)).quantity(new Quantity(2)).build()))
                .build();
    }
}
