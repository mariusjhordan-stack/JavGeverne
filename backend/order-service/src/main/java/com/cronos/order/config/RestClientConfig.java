package com.cronos.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${app.catalog-service-url}")
    private String catalogServiceUrl;

    @Bean
    public RestClient catalogRestClient() {
        return RestClient.builder()
                .baseUrl(catalogServiceUrl)
                .build();
    }
}
