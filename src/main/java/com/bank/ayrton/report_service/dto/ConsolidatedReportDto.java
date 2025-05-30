package com.bank.ayrton.report_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConsolidatedReportDto {
    private ClientDto client;
    private List<ProductDto> products;
    private List<MovementDto> movements;

}
