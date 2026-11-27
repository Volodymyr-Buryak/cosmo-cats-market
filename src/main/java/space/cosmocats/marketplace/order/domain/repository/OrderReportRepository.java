package space.cosmocats.marketplace.order.domain.repository;

import java.util.List;
import java.util.Set;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.order.domain.model.PopularProduct;

public interface OrderReportRepository {
    List<PopularProduct> findPopularProducts(Set<OrderStatus> statuses, int limit);
}
