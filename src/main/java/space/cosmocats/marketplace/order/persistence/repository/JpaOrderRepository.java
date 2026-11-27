package space.cosmocats.marketplace.order.persistence.repository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import space.cosmocats.marketplace.order.persistence.entity.OrderEntity;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, UUID> {
    @Override
    @EntityGraph(attributePaths = "items")
    Optional<OrderEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "items")
    List<OrderEntity> findAll(Sort sort);

    @Query("""
            select i.productId as productId, p.name as productName, sum(i.quantity) as quantitySold
            from OrderItemEntity i join i.order o
            join ProductEntity p on p.id = i.productId
            where o.status in :statuses
            group by i.productId, p.name
            order by sum(i.quantity) desc, i.productId asc
            """)
    List<PopularProductProjection> findPopularProducts(
            @Param("statuses") java.util.Set<space.cosmocats.marketplace.order.domain.model.OrderStatus> statuses,
            org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderEntity o where o.id = :id")
    Optional<OrderEntity> findByIdForUpdate(@Param("id") UUID id);
}
