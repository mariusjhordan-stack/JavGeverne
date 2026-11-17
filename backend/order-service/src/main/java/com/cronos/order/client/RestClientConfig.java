package com.cronos.order.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static RestClient client(String baseUrl, String internalToken) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(8000);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader("X-Internal-Token", internalToken).build();
    }

    @Bean
    public RestClient catalogRestClient(@Value("${app.catalog-service-url}") String url,
                                        @Value("${app.security.internal-token}") String token) {
        return client(url, token);
    }

    @Bean
    public RestClient notificationRestClient(@Value("${app.notification-service-url}") String url,
                                             @Value("${app.security.internal-token}") String token) {
        return client(url, token);
    }
}
