package com.cronos.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class RestClientConfig {

    private static SimpleClientHttpRequestFactory timeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(4000);
        factory.setReadTimeout(12000);
        return factory;
    }

    @Bean("stripeRestClient")
    public RestClient stripeRestClient(StripeProperties properties) {
        return RestClient.builder().baseUrl(properties.apiBaseUrl()).requestFactory(timeouts()).build();
    }

    @Bean("orderServiceRestClient")
    public RestClient orderServiceRestClient(@Value("${app.order-service-url}") String url,
                                             @Value("${app.security.internal-token}") String token) {
        return RestClient.builder().baseUrl(url).requestFactory(timeouts())
                .defaultHeader("X-Internal-Token", token).build();
    }
}
