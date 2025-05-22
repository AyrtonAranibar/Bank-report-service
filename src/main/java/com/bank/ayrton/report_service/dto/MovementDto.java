package com.bank.ayrton.report_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MovementDto {
    private String id;
    private String clientId;
    private String productId;
    private MovementType type; // DEPOSIT o WITHDRAWAL
    private Double amount;
    private LocalDateTime date;
    private Double commission;
}
