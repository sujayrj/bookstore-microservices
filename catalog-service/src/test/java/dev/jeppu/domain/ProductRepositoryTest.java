package dev.jeppu.domain;

import java.util.List;

import dev.jeppu.TestcontainersConfiguration;
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
}
