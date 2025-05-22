package com.bank.ayrton.report_service.api.report;

import com.bank.ayrton.report_service.dto.CommissionReportDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

public interface ReportService {

    // reporte de saldos promedio diarios del mes en curso -cliente
    Mono<Double> getAverageDailyBalance(String clientId);

    //reporte de comisiones covradas por producto en un rango de fechas
    Flux<CommissionReportDto> getCommissionReport(String productId, LocalDate startDate, LocalDate endDate);
}