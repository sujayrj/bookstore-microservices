package dev.jeppu.bookstore.domain;

public class ProductMapper {
    public static Product toDTO(ProductEntity entity) {
        return new Product(
                entity.getCode(), entity.getName(), entity.getDescription(), entity.getImageUrl(), entity.getPrice());
    }

    public static ProductEntity toEntity(Product product) {
        ProductEntity productEntity = new ProductEntity();
        productEntity.setCode(productEntity.getCode());
        productEntity.setName(productEntity.getName());
        productEntity.setDescription(productEntity.getDescription());
        productEntity.setImageUrl(productEntity.getImageUrl());
        productEntity.setPrice(productEntity.getPrice());
        return productEntity;
    }
}
