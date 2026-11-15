package com.cronos.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean("mercadoPagoRestClient")
    public RestClient mercadoPagoRestClient(MercadoPagoConfig config) {
        return RestClient.builder()
                .baseUrl(config.getApiBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean("orderServiceRestClient")
    public RestClient orderServiceRestClient(
            @Value("${services.order-service-url}") String orderServiceUrl) {
        return RestClient.builder()
                .baseUrl(orderServiceUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
