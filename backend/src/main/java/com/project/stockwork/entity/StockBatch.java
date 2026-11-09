package com.project.stockwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("stock_batch")
public class StockBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long warehouseMaterialId;

    private String batchNo;

    private BigDecimal remainStock;

    private Date createTime;

    private Date lastIoTime;
}
