package com.bank.ayrton.report_service.controller;

import com.bank.ayrton.report_service.dto.CommissionReportDto;
import com.bank.ayrton.report_service.api.report.ReportService;
import com.bank.ayrton.report_service.dto.ConsolidatedReportDto;
import com.bank.ayrton.report_service.dto.MovementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // Obtener saldo promedio diario del cliente
    @GetMapping("/average-balance/{clientId}")
    public Mono<Double> getAverageBalance(@PathVariable String clientId) {
        return reportService.getAverageDailyBalance(clientId);
    }

    // Obtener comisiones cobradas por producto en un rango de fechas
    @GetMapping("/commission-report")
    public Flux<CommissionReportDto> getCommissionReport(
            @RequestParam String productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return reportService.getCommissionReport(productId, startDate, endDate);
    }

    @GetMapping("/consolidated/{clientId}")
    public Mono<ConsolidatedReportDto> getConsolidatedReport(@PathVariable String clientId) {
        return reportService.getConsolidatedReport(clientId);
    }

    @GetMapping("/card/{cardId}/movements")
    public Flux<MovementDto> getLast10CardMovements(@PathVariable String cardId) {
        return reportService.getLast10CardMovements(cardId);
    }
}