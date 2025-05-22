package com.bank.ayrton.report_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CommissionReportDto {
    private String productId;
    private double totalCommission;

}
