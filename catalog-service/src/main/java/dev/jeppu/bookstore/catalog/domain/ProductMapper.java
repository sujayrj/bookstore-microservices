package dev.jeppu.bookstore.catalog.domain;

import org.springframework.stereotype.Service;

@Service
public class ProductMapper {
    public Product toProduct(ProductEntity productEntity) {
        return new Product(productEntity.getCode(),
                productEntity.getName(),
                productEntity.getDescription(),
                productEntity.getImageUrl(),
                productEntity.getPrice());
    }
}
