package space.cosmocats.marketplace.product.web;

import java.util.UUID;
import java.util.Currency;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import space.cosmocats.marketplace.common.pagination.PageResult;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;
import space.cosmocats.marketplace.product.web.dto.request.ProductRequest;
import space.cosmocats.marketplace.product.web.dto.response.ProductResponse;
import space.cosmocats.marketplace.product.domain.repository.ProductCriteria;
import space.cosmocats.marketplace.product.web.dto.request.ProductPageRequest;
import space.cosmocats.marketplace.product.web.dto.response.ProductPageResponse;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "page", defaultValue = "0")
    @Mapping(target = "size", defaultValue = "20")
    @Mapping(target = "sort", defaultValue = "PRICE_ASC")
    ProductCriteria toCriteria(ProductPageRequest request);

    @Mapping(target = "stock", source = "stock.value")
    @Mapping(target = "price", source = "price.amount")
    @Mapping(target = "currency", source = "price.currency.currencyCode")
    ProductResponse toResponse(Product product);

    ProductPageResponse toPageResponse(PageResult<Product> page);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "price", expression = "java(toMoney(request.price(), request.currency()))")
    Product toDomain(ProductRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "price", expression = "java(toMoney(request.price(), request.currency()))")
    Product toDomain(UUID id, ProductRequest request);

    default Money toMoney(BigDecimal amount, String currency) {
        return new Money(Currency.getInstance(currency), amount);
    }

    default Quantity toQuantity(Integer stock) {
        return new Quantity(stock);
    }

}
