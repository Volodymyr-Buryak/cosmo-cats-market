package space.cosmocats.marketplace.order.persistence.entity;

import lombok.*;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "order_items", schema = "public")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order-items")
    @SequenceGenerator(name = "order-items", sequenceName = "public.order_items_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private OrderEntity order;
    private int position;
    private UUID productId;

    private String productName;

    private BigDecimal unitPriceAmount;
    private int quantity;

    @Builder
    public OrderItemEntity(
            OrderEntity order,
            int position,
            UUID productId,
            String productName,
            BigDecimal unitPriceAmount,
            int quantity
    ) {
        this.order = order;
        this.position = position;
        this.productId = productId;
        this.productName = productName;
        this.unitPriceAmount = unitPriceAmount;
        this.quantity = quantity;
    }

}
