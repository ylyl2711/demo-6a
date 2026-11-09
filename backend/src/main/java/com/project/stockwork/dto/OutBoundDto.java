package com.project.stockwork.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OutBoundDto {
    private Long materialId;
    private BigDecimal operNum;
    /** 指定批次；不传则按 FIFO 自动分配 */
    private Long batchId;
    /** 生产领料时关联工单 */
    private Long workOrderId;
    private String remark;
}
