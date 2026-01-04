package dev.jeppu.domain;

import dev.jeppu.TestcontainersConfiguration;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
@DataJpaTest(
        properties = {
            "spring.test.database.replace=none",
            // "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///db",
        })
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {
    @Autowired
    private ProductRepository productRepository;

    @Test
    public void shouldGetAllProducts() {
        List<ProductEntity> allProducts = productRepository.findAll();
        Assertions.assertThat(allProducts).hasSize(15);
    }

    @Test
    public void shouldGetProductForCode() {
        Optional<ProductEntity> productEntity = productRepository.findProductEntityByCode("P104");
        Assertions.assertThat(productEntity).isNotEmpty();
        Assertions.assertThat(productEntity.get().getId()).isNotNull();
        Assertions.assertThat(productEntity.get().getCode()).isEqualTo("P104");
        Assertions.assertThat(productEntity.get().getName()).isEqualTo("The Fault in Our Stars");
        Assertions.assertThat(productEntity.get().getDescription()).isNotEmpty();
    }

    @Test
    public void shouldReturnEmptyWhenProductDoesNotExist() {
        Optional<ProductEntity> optionalProductEntity = productRepository.findProductEntityByCode("P111111111");
        Assertions.assertThat(optionalProductEntity).isEmpty();
    }
}
