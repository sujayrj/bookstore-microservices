package dev.jeppu.domain;

class ProductMapper {
    static ProductResponseDTO mapToProductResponse(ProductEntity productEntity) {
        return new ProductResponseDTO(
                productEntity.getCode(),
                productEntity.getName(),
                productEntity.getDescription(),
                productEntity.getPrice(),
                productEntity.getImageUrl());
    }
}
