package space.cosmocats.marketplace.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DatabaseConstraintsIntegrationTest extends PostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Applies common Liquibase migrations and test fixtures to PostgreSQL")
    void shouldApplyMigrationsAndTestFixtures() {
        String database = jdbcTemplate.queryForObject("select current_database()", String.class);
        Integer products = jdbcTemplate.queryForObject("select count(*) from products", Integer.class);
        Integer migrations = jdbcTemplate.queryForObject("select count(*) from databasechangelog", Integer.class);

        assertThat(database).isEqualTo("marketplace_test");
        assertThat(products).isEqualTo(4);
        assertThat(migrations).isPositive();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "update products set name = ' '",
            "update products set description = ' '",
            "update products set price_amount = 0",
            "update products set price_amount = -1",
            "update products set price_amount = 'NaN'::numeric",
            "update products set price_amount = 'Infinity'::numeric",
            "update products set price_amount = '-Infinity'::numeric",
            "update products set price_currency = 'usd'",
            "update products set stock = -1",
            "update products set category_id = '00000000-0000-0000-0000-000000000099'::uuid",
            "update products set name = null",
            "update categories set parent_id = id",
            "update categories set name = ' '",
            "update categories set name = 'Duplicate'"
    })
    @DisplayName("Rejects invalid catalog data directly at the SQL boundary")
    void shouldRejectInvalidCatalogData(String sql) {
        assertThatThrownBy(() -> jdbcTemplate.update(sql))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNKNOWN", "", "paid"})
    @DisplayName("Rejects unknown order statuses in PostgreSQL")
    void shouldRejectInvalidOrderStatus(String status) {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into orders (id,buyer_id,created_at,status,currency)
                values (gen_random_uuid(),gen_random_uuid(),now(),?,'USD')
                """, status)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "position = -1",
            "quantity = 0",
            "quantity = -1",
            "unit_price_amount = 0",
            "unit_price_amount = 'NaN'::numeric",
            "product_name = ' '"
    })
    @DisplayName("Rejects invalid order positions in PostgreSQL")
    void shouldRejectInvalidOrderPosition(String assignment) {
        jdbcTemplate.update("""
                insert into orders (id,buyer_id,created_at,status,currency)
                values ('00000000-0000-0000-0000-000000000001',gen_random_uuid(),now(),'CREATED','USD')
                """);

        jdbcTemplate.update("""
                insert into order_items (order_id,position,product_id,product_name,unit_price_amount,quantity)
                values ('00000000-0000-0000-0000-000000000001',0,
                        '30000000-0000-4000-8000-000000000001','Snapshot',10,1)
                """);

        assertThatThrownBy(() -> jdbcTemplate.update("update order_items set " + assignment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
