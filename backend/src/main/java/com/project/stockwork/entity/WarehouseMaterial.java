package com.project.stockwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("warehouse_material")
public class WarehouseMaterial {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String warehouseName;

    private String baseSite;

    private String materialCode;

    private String materialName;

    private Integer materialType;

    private BigDecimal totalStock;

    private BigDecimal stockMin;

    private BigDecimal stockMax;

    private Integer stagnantDay;
}
