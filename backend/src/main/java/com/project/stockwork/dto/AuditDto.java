package com.project.stockwork.dto;

import lombok.Data;

@Data
public class AuditDto {
    private Long workOrderId;
    /** 1-合格 2-不合格 */
    private Integer result;
    private String remark;
    private Long badReasonId;
}
