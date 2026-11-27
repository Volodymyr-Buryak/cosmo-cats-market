package space.cosmocats.marketplace.integration;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import space.cosmocats.marketplace.order.application.service.OrderReportService;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import space.cosmocats.marketplace.product.persistence.repository.CategoryRepositoryAdapter;
import space.cosmocats.marketplace.product.persistence.mapper.CategoryPersistenceMapperImpl;
import space.cosmocats.marketplace.order.persistence.repository.OrderReportRepositoryAdapter;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        CategoryRepositoryAdapter.class, CategoryPersistenceMapperImpl.class,
        OrderReportRepositoryAdapter.class, OrderReportService.class
})
class AdditionalAssignmentsIntegrationTest extends PostgreSqlIntegrationTest {

    private static final UUID PRODUCT_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");

    @Autowired
    private CategoryRepositoryAdapter categories;

    @Autowired
    private OrderReportService reports;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("Finds a category by natural ID after renaming it")
    void shouldResolveRenamedNaturalId() {
        var original = categories.findByName("Test food").orElseThrow();
        categories.update(original.toBuilder().name("Renamed food").build());
        entityManager.flush();
        entityManager.clear();

        var renamed = categories.findByName("Renamed food").orElseThrow();

        assertThat(renamed.getId()).isEqualTo(original.getId());
        assertThat(categories.findByName("Test food")).isEmpty();
        assertThat(categories.findByName("Missing category")).isEmpty();
    }

    @Test
    @DisplayName("Ranks purchased quantities and excludes created and cancelled orders")
    void shouldRankPurchasedQuantities() {
        jdbc.update("delete from order_items");
        jdbc.update("delete from orders");
        addPurchase("CREATED", PRODUCT_ID, 100);
        addPurchase("CANCELLED", PRODUCT_ID, 100);
        addPurchase("PAID", PRODUCT_ID, 2);
        addPurchase("SHIPPED", PRODUCT_ID, 3);
        addPurchase("DELIVERED", PRODUCT_ID, 4);
        UUID other = UUID.fromString("30000000-0000-4000-8000-000000000002");
        addPurchase("PAID", other, 9);

        var result = reports.getPopularProducts(1);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().productId()).isEqualTo(PRODUCT_ID);
        assertThat(result.getFirst().quantitySold()).isEqualTo(9);
        assertThat(result.getFirst().productName()).isEqualTo("Star Lamp");
        assertThat(reports.getPopularProducts(10)).hasSize(2);
    }

    private void addPurchase(String status, UUID productId, int quantity) {
        UUID orderId = UUID.randomUUID();

        jdbc.update("insert into orders (id,buyer_id,created_at,status,currency) values (?,?,now(),?,'USD')",
                orderId, orderId, status);

        jdbc.update("""
                insert into order_items (id,order_id,position,product_id,product_name,unit_price_amount,quantity)
                values (nextval('order_items_id_seq'),?,0,?,'Historical name',10,?)
                """, orderId, productId, quantity);
    }
}
