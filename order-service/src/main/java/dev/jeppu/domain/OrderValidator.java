package dev.jeppu.domain;

import dev.jeppu.client.Product;
import dev.jeppu.client.ProductServiceClient;
import dev.jeppu.domain.exception.InvalidOrderException;
import dev.jeppu.domain.models.CreateOrderRequest;
import dev.jeppu.domain.models.OrderItem;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderValidator {
    private final ProductServiceClient productServiceClient;

    public void validateOrder(CreateOrderRequest orderRequest) {
        for (OrderItem item : orderRequest.items()) {
            Optional<Product> optionalProduct = productServiceClient.validateOrder(item.code());
            if (optionalProduct.isEmpty()) {
                log.error("Product with code : " + item.code() + " is not available in Catalog service");
                throw new InvalidOrderException("Product with code : " + item.code() + " is not available");
            }
            if (item.price().compareTo(optionalProduct.get().price()) != 0) {
                log.error(
                        "Product price does not match. Code : {} , Input price : {} , Catalog price : {}",
                        item.code(),
                        item.price(),
                        optionalProduct.get().price());
                throw new InvalidOrderException(
                        "Product price does not match. Code : " + item.code() + ". Input price : " + item.price()
                                + " , Catalog price : " + optionalProduct.get().price());
            }
        }
    }
}
