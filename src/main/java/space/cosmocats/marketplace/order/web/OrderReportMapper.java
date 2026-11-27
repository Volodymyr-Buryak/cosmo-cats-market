package space.cosmocats.marketplace.order.web;

import org.mapstruct.Mapper;
import space.cosmocats.marketplace.order.domain.model.PopularProduct;
import space.cosmocats.marketplace.order.web.dto.PopularProductResponse;

@Mapper
public interface OrderReportMapper {
    PopularProductResponse toResponse(PopularProduct product);
}
