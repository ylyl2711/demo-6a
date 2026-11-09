package com.project.stockwork.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkOrderSaveDto {
    private Long id;
    private String baseSite;
    private Long materialId;
    private BigDecimal planNum;
    private Long operatorUserId;
    private String productBatch;
}
