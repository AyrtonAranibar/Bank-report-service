package com.bank.ayrton.report_service.service.report;

import com.bank.ayrton.report_service.api.report.ReportService;
import com.bank.ayrton.report_service.dto.CommissionReportDto;
import com.bank.ayrton.report_service.dto.MovementDto;
import com.bank.ayrton.report_service.dto.ProductDto;
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
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final WebClient clientWebClient;
    private final WebClient productWebClient;
    private final WebClient movementWebClient;

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
}
