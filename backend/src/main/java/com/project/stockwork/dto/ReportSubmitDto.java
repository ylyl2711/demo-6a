package com.project.stockwork.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReportSubmitDto {
    private Long workOrderId;
    private Long processId;
    private BigDecimal goodNum;
    private BigDecimal badNum;
    private Long badReasonId;
    private String photoUrl;
}
