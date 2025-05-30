package com.bank.ayrton.report_service.service.report;

import com.bank.ayrton.report_service.api.report.ReportService;
import com.bank.ayrton.report_service.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final WebClient clientWebClient;
    private final WebClient productWebClient;
    private final WebClient movementWebClient;
    private final WebClient debitCardWebClient;

    public Flux<Map<String, Double>> getAverageBalanceByClient(String clientId) {
        log.info("Generando reporte de saldo promedio diario para cliente: {}", clientId);

        return productWebClient.get()
                .uri("/api/v1/product/client/{id}", clientId)
                .retrieve()
                .bodyToFlux(ProductDto.class)
                .flatMap(product -> {
                    LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

                    return movementWebClient.get()
                            .uri("/api/v1/movement/client/{id}", clientId)
                            .retrieve()
                            .bodyToFlux(MovementDto.class)
                            .filter(movement -> movement.getDate().isAfter(startOfMonth) &&
                                    movement.getProductId().equals(product.getId()))
                            .collectList()
                            .map(movements -> {
                                double average = movements.stream()
                                        .collect(Collectors.averagingDouble(MovementDto::getAmount));
                                return Map.of(product.getId(), average);
                            });
                });
    }

    public Flux<Map<String, Double>> getCommissionReportByProduct(String productId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Generando reporte de comisiones para producto: {} entre {} y {}", productId, startDate, endDate);

        return movementWebClient.get()
                .uri("/api/v1/movement/client/{id}", productId)
                .retrieve()
                .bodyToFlux(MovementDto.class)
                .filter(movement -> movement.getProductId().equals(productId) &&
                        movement.getDate().isAfter(startDate) &&
                        movement.getDate().isBefore(endDate) &&
                        movement.getCommission() != null && movement.getCommission() > 0)
                .collectList()
                .map(movements -> {
                    double totalCommission = movements.stream()
                            .mapToDouble(MovementDto::getCommission)
                            .sum();
                    return Map.of(productId, totalCommission);
                }).flux()
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron comisiones en el periodo")));
    }

    @Override
    public Mono<Double> getAverageDailyBalance(String clientId) {
        log.info("Calculando promedio diario de saldos para cliente: {}", clientId);

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        return productWebClient.get()
                .uri("/api/v1/product/client/{id}", clientId)
                .retrieve()
                .bodyToFlux(ProductDto.class)
                .flatMap(product -> movementWebClient.get()
                        .uri("/api/v1/movement/client/{id}", clientId)
                        .retrieve()
                        .bodyToFlux(MovementDto.class)
                        .filter(movement -> movement.getDate().isAfter(startOfMonth) &&
                                movement.getProductId().equals(product.getId()))
                )
                .map(MovementDto::getAmount)
                .collect(Collectors.averagingDouble(amount -> amount));
    }

    @Override
    public Flux<CommissionReportDto> getCommissionReport(String productId, LocalDate startDate, LocalDate endDate) {
        log.info("Generando DTO de reporte de comisiones para producto {} entre {} y {}", productId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        return movementWebClient.get()
                .uri("/api/v1/movement/client/{id}", productId)
                .retrieve()
                .bodyToFlux(MovementDto.class)
                .filter(movement -> movement.getProductId().equals(productId) &&
                        movement.getDate().isAfter(startDateTime) &&
                        movement.getDate().isBefore(endDateTime) &&
                        movement.getCommission() != null && movement.getCommission() > 0)
                .collectList()
                .map(movements -> {
                    double total = movements.stream().mapToDouble(MovementDto::getCommission).sum();
                    CommissionReportDto dto = new CommissionReportDto(productId, total);
                    return dto;
                })
                .flux();
    }

    @Override
    public Mono<ConsolidatedReportDto> getConsolidatedReport(String clientId) {
        Mono<ClientDto> clientMono = clientWebClient.get()
                .uri("/api/v1/client/{id}", clientId)
                .retrieve()
                .bodyToMono(ClientDto.class);

        Flux<ProductDto> productsFlux = productWebClient.get()
                .uri("/api/v1/product/client/{clientId}", clientId)
                .retrieve()
                .bodyToFlux(ProductDto.class);

        Flux<MovementDto> movementsFlux = movementWebClient.get()
                .uri("/api/v1/movement/client/{clientId}", clientId)
                .retrieve()
                .bodyToFlux(MovementDto.class);

        return Mono.zip(clientMono, productsFlux.collectList(), movementsFlux.collectList())
                .map(tuple -> {
                    ConsolidatedReportDto dto = new ConsolidatedReportDto();
                    dto.setClient(tuple.getT1());
                    dto.setProducts(tuple.getT2());
                    dto.setMovements(tuple.getT3());
                    return dto;
                });
    }

    @Override
    public Flux<MovementDto> getLast10CardMovements(String cardId) {
        return debitCardWebClient.get()
                .uri("/{id}", cardId)
                .retrieve()
                .bodyToMono(DebitCardDto.class)
                .flatMapMany(card -> {
                    List<String> accountIds = card.getLinkedAccountIds();

                    List<Mono<List<MovementDto>>> movementsByAccount = accountIds.stream()
                            .map(accountId ->
                                    movementWebClient.get()
                                            .uri("/product/{productId}", accountId)
                                            .retrieve()
                                            .bodyToFlux(MovementDto.class)
                                            .collectList()
                            )
                            .toList();

                    return Flux.merge(movementsByAccount)
                            .flatMap(Flux::fromIterable)
                            .sort((m1, m2) -> m2.getDate().compareTo(m1.getDate())) // orden descendente
                            .take(10);
                });
    }


}
