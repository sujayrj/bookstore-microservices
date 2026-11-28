package dev.jeppu.client;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ProductServiceClient {
    private final RestClient catalogServiceRestClient;

    public Optional<Product> validateOrder(String code) {
        Product product = catalogServiceRestClient
                .get()
                .uri("/api/v1/products/{code}")
                .retrieve()
                .body(Product.class);
        return Optional.ofNullable(product);
    }
}
