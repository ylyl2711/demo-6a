package com.project.stockwork.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InBoundDto {
    private Long materialId;
    private BigDecimal operNum;
    private String batchNo;
    private String remark;
    /** 工单成品入库时关联工单 */
    private Long workOrderId;
}
