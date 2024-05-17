package dev.jeppu.bookstore.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
@DataJpaTest(
        properties = {"spring.test.database.replace=none", "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///db"})
class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    @Test
    void verifyAllProducts() {
        List<ProductEntity> productEntityList = productRepository.findAll();
        assertThat(productEntityList.size()).isEqualTo(15);
    }

    @Test
    void verifyProductByCode() {
        String code = "P100";
        Optional<ProductEntity> actualProduct = productRepository.findByCode(code);
        assertThat(actualProduct.isPresent()).isEqualTo(true);
        assertThat(actualProduct.get().getCode()).isEqualTo("P100");
        assertThat(actualProduct.get().getName()).isEqualTo("The Hunger Games");
        assertThat(actualProduct.get().getDescription())
                .isEqualTo("Winning will make you famous. Losing means certain death...");
        assertThat(actualProduct.get().getPrice()).isEqualTo(BigDecimal.valueOf(34.0));
    }

    @Test
    void verifyProductByCodeIsNotFound() {
        String code = "P1000000";
        Optional<ProductEntity> actualProductEntity = productRepository.findByCode(code);
        assertThat(actualProductEntity.isPresent()).isEqualTo(false);
    }
}
