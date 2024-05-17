package dev.jeppu.bookstore.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

@Import(ContainersConfig.class)
class CatalogServiceApplicationTests extends AbstractIntTest {

    @Test
    void contextLoads() {}
}
