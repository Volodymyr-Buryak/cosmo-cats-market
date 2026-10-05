package space.cosmocats.marketplace.product.persistence;

import java.util.Currency;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.domain.Page;
import space.cosmocats.marketplace.product.domain.model.Product;
import space.cosmocats.marketplace.common.pagination.PageResult;
import space.cosmocats.marketplace.product.domain.model.value.Money;
import space.cosmocats.marketplace.product.domain.model.value.Quantity;

@Mapper
public interface ProductPersistenceMapper {

    @Mapping(target = "price", expression = "java(toMoney(entity.getPrice(), entity.getCurrency()))")
    @Mapping(target = "stock", expression = "java(toQuantity(entity.getStock()))")
    @Mapping(target = "categoryId", source = "categoryIdRef")
    Product toDomain(ProductEntity entity);

    @Mapping(target = "price", source = "price.amount")
    @Mapping(target = "currency", source = "price.currency.currencyCode")
    @Mapping(target = "stock", source = "stock.value")
    @Mapping(target = "categoryIdRef", source = "categoryId")
    ProductEntity toEntity(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "price", source = "price.amount")
    @Mapping(target = "currency", source = "price.currency.currencyCode")
    @Mapping(target = "stock", source = "stock.value")
    @Mapping(target = "categoryIdRef", source = "categoryId")
    void updateEntity(Product product, @MappingTarget ProductEntity entity);

    @Mapping(target = "page", source = "number")
    @Mapping(target = "hasContent", expression = "java(page.hasContent())")
    PageResult<Product> toPageResult(Page<ProductEntity> page);

    default Money toMoney(BigDecimal amount, String currency) {
        return new Money(Currency.getInstance(currency), amount);
    }

    default Quantity toQuantity(Integer stock) {
        return new Quantity(stock);
    }

}
