package dev.jeppu.domain.product;

public class ProductMapper {
    public static Product toProduct(ProductEntity productEntity) {
        Product product = new Product(
                productEntity.getCode(),
                productEntity.getName(),
                productEntity.getDescription(),
                productEntity.getImageUrl(),
                productEntity.getPrice());
        return product;
    }

    public static ProductEntity toProductEntity(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setCode(product.code());
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setPrice(product.price());
        entity.setImageUrl(product.imageUrl());
        return entity;
    }
}
