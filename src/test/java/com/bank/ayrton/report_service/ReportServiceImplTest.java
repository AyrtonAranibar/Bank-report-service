package com.bank.ayrton.report_service;

import com.bank.ayrton.report_service.dto.CommissionReportDto;
import com.bank.ayrton.report_service.dto.MovementDto;
import com.bank.ayrton.report_service.dto.ProductDto;
import com.bank.ayrton.report_service.service.report.ReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

public class ReportServiceImplTest {

    private WebClient productWebClient;
    private WebClient movementWebClient;
    private ReportServiceImpl reportService;

    @BeforeEach
    void setUp() {
        productWebClient = mock(WebClient.class, RETURNS_DEEP_STUBS);
        movementWebClient = mock(WebClient.class, RETURNS_DEEP_STUBS);
        reportService = new ReportServiceImpl(null, productWebClient, movementWebClient);
    }

    @Test
    void testGetAverageDailyBalance() {
        ProductDto product1 = new ProductDto();
        product1.setId("1");
        product1.setBalance(100.0);

        MovementDto movement1 = new MovementDto();
        movement1.setProductId("1");
        movement1.setAmount(100.0);
        movement1.setDate(LocalDateTime.now());

        when(productWebClient.get()
                .uri(anyString(), any(Object[].class))
                .retrieve()
                .bodyToFlux(ProductDto.class))
                .thenReturn(Flux.just(product1));

        when(movementWebClient.get()
                .uri(anyString(), any(Object[].class))
                .retrieve()
                .bodyToFlux(MovementDto.class))
                .thenReturn(Flux.just(movement1));

        Mono<Double> result = reportService.getAverageDailyBalance("dummyId");

        StepVerifier.create(result)
                .expectNext(100.0)
                .verifyComplete();
    }

    @Test
    void testGetCommissionReport() {
        MovementDto movement1 = new MovementDto();
        movement1.setProductId("dummyProduct");
        movement1.setDate(LocalDateTime.now());
        movement1.setCommission(5.0);

        MovementDto movement2 = new MovementDto();
        movement2.setProductId("dummyProduct");
        movement2.setDate(LocalDateTime.now());
        movement2.setCommission(10.0);

        when(movementWebClient.get()
                .uri(anyString(), any(Object[].class))
                .retrieve()
                .bodyToFlux(MovementDto.class))
                .thenReturn(Flux.just(movement1, movement2));

        Flux<CommissionReportDto> result = reportService.getCommissionReport("dummyProduct", LocalDate.now(), LocalDate.now());

        StepVerifier.create(result)
                .expectNextMatches(dto -> dto.getTotalCommission() == 15.0 && dto.getProductId().equals("dummyProduct"))
                .verifyComplete();
    }
}
