package space.cosmocats.marketplace.order.application.service;

import java.util.Set;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;
import space.cosmocats.marketplace.order.domain.model.PopularProduct;
import space.cosmocats.marketplace.order.domain.repository.OrderReportRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderReportService {

    private static final Set<OrderStatus> PURCHASED_STATUSES = Set.of(
            OrderStatus.PAID,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED
    );

    private final OrderReportRepository repository;

    public List<PopularProduct> getPopularProducts(int limit) {
        return repository.findPopularProducts(PURCHASED_STATUSES, limit);
    }

}
