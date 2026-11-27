package space.cosmocats.marketplace.product.persistence.entity;

import lombok.*;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.persistence.*;
import space.cosmocats.marketplace.common.persistence.BaseEntity;

@Entity
@Table(name = "products", schema = "public")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity extends BaseEntity {
    private String name;

    private String description;

    @Column(name = "price_amount")
    private BigDecimal price;
    @Column(name = "price_currency")
    private String currency;
    private Integer stock;

    @Column(name = "category_id")
    private UUID categoryIdRef;

    @Builder
    public ProductEntity(
            UUID id, String name, String description, BigDecimal price,
            String currency, Integer stock, UUID categoryIdRef
    ) {
        super(id);
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.currency = currency;
        this.categoryIdRef = categoryIdRef;
        this.description = description;
    }

}
