package com.project.stockwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("stock_bill")
public class StockBill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String billNo;

    private Integer billType;

    private Long warehouseMaterialId;

    private Long batchId;

    private BigDecimal operNum;

    private String operUser;

    private Date billTime;

    private String remark;

    private Long workOrderId;

    private Integer billStatus;
}
