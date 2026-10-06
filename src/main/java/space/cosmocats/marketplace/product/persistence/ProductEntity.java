package space.cosmocats.marketplace.product.persistence;

import lombok.*;
import java.util.UUID;
import java.util.Objects;
import java.math.BigDecimal;
import jakarta.persistence.*;
import org.hibernate.proxy.HibernateProxy;

@Entity
@Table(name = "products", schema = "public")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity {
    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    private BigDecimal price;
    private String currency;
    private Integer stock;
    private UUID categoryIdRef;

    @Builder
    public ProductEntity(
            UUID id, String name, String description, BigDecimal price,
            String currency, Integer stock, UUID categoryIdRef
    ) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.currency = currency;
        this.categoryIdRef = categoryIdRef;
        this.description = description;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductEntity product)) return false;
        if (getEffectiveClass(this) != getEffectiveClass(product)) return false;
        return id != null && Objects.equals(id, product.id);
    }

    @Override
    public final int hashCode() {
        return getEffectiveClass(this).hashCode();
    }

    private static Class<?> getEffectiveClass(Object object) {
        return (object instanceof HibernateProxy proxy)
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : object.getClass();
    }
}
