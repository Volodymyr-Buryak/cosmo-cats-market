package space.cosmocats.marketplace.order.persistence.repository;

import java.util.UUID;

public interface PopularProductProjection {
    UUID getProductId();
    String getProductName();
    Long getQuantitySold();
}
