package space.cosmocats.marketplace.order.persistence.entity;

import lombok.*;
import java.util.UUID;
import java.util.List;
import java.time.Instant;
import java.util.ArrayList;
import jakarta.persistence.*;
import space.cosmocats.marketplace.common.persistence.BaseEntity;
import space.cosmocats.marketplace.order.domain.model.OrderStatus;

@Entity
@Table(name = "orders", schema = "public")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity extends BaseEntity {
    private UUID buyerId;
    private Instant createdAt;

    @Setter
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private String currency;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<OrderItemEntity> items = new ArrayList<>();

    @Builder
    public OrderEntity(UUID id, UUID buyerId, Instant createdAt, OrderStatus status, String currency) {
        super(id);
        this.buyerId = buyerId;
        this.createdAt = createdAt;
        this.status = status;
        this.currency = currency;
    }

}
