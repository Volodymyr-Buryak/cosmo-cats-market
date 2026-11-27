package space.cosmocats.marketplace.order.persistence.repository;

import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.PageRequest;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.order.domain.repository.OrderReportRepository;
import space.cosmocats.marketplace.order.domain.model.PopularProduct;

@Repository
@RequiredArgsConstructor
public class OrderReportRepositoryAdapter implements OrderReportRepository {
    private final JpaOrderRepository repository;

    @Override
    public List<PopularProduct> findPopularProducts(Set<OrderStatus> statuses, int limit) {
        return repository.findPopularProducts(statuses, PageRequest.of(0, limit)).stream()
                .map(row -> new PopularProduct(row.getProductId(), row.getProductName(), row.getQuantitySold()))
                .toList();
    }
}
