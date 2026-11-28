package dev.jeppu.client;

import dev.jeppu.config.ApplicationPropertiesConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ProductServiceClientConfig {
    private final ApplicationPropertiesConfig propertiesConfig;

    @Bean(name = "catalogServiceRestClient")
    RestClient productServiceRestClient() {
        return RestClient.builder()
                .baseUrl(propertiesConfig.catalogServiceBaseUrl())
                .build();
    }
}
