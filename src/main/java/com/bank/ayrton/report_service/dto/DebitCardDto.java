package com.bank.ayrton.report_service.dto;

import lombok.Data;

import java.util.List;

@Data
public class DebitCardDto {
    private String id;
    private String clientId;
    private String mainAccountId;
    private List<String> linkedAccountIds;
}