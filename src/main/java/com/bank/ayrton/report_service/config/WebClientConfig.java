package com.bank.ayrton.report_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient clientWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8081") // URL del servicio cliente
                .build();
    }

    @Bean
    public WebClient productWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8082") // URL del servicio producto
                .build();
    }

    @Bean
    public WebClient movementWebClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8083") // URL del servicio movement
                .build();
    }

    @Bean
    public WebClient debitCardWebClient() {
        return WebClient.builder()
                .baseUrl("http://product-service/api/v1/debit-card")
                .build();
    }
}
