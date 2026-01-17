package dev.jeppu.bookstore.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest(
        properties = {"spring.test.database.replace=none", "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///db"})
@Sql("/test-data.sql")
class ProductRepositoryTest {
    @Autowired
    private ProductRepository repository;

    @Test
    public void testFindAllProducts() {
        List<ProductEntity> allProducts = repository.findAll();
        Assertions.assertThat(allProducts).isNotNull().isNotEmpty();
        Assertions.assertThat(allProducts.size()).isEqualTo(14);
    }

    @Test
    public void testFindProductByCode() {
        Optional<ProductEntity> optionalProduct = repository.findProductEntityByCode("P100");
        Assertions.assertThat(optionalProduct).isNotEmpty();
        ProductEntity productEntity = optionalProduct.get();
        Assertions.assertThat(productEntity.getCode()).isEqualTo("P100");
    }
}
