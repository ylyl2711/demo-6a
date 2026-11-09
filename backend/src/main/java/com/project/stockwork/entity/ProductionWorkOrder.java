package com.project.stockwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("production_work_order")
public class ProductionWorkOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String workOrderNo;

    private String baseSite;

    private Long materialId;

    private BigDecimal planNum;

    private Long operatorUserId;

    private Integer workStatus;

    private String productBatch;

    private Date createTime;
}
